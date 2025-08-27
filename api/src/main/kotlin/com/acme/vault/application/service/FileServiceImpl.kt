package com.acme.vault.application.service

import com.acme.vault.adapter.persistance.FileRepository
import com.acme.vault.domain.events.FileUploadedEvent
import com.acme.vault.domain.models.File
import com.acme.vault.domain.models.FileStatus
import com.acme.vault.domain.service.IFileService
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import java.util.UUID

@Service
class FileServiceImpl(
    private val fileRepository: FileRepository,
    private val minioService: MinioService,
    private val eventPublisher: EventPublisherService,
    private val cacheService: CacheService
) : IFileService {

    override fun createFile(file: File): Mono<File?> {
        println("=== FILE SERVICE: createFile() called for: ${file.originalName} ===")
        
        // Verificar si ya existe un archivo con el mismo hash
        return fileRepository.findByFileHash(file.fileHash)
            .flatMap<File?> { existingFile ->
                println("File with hash ${file.fileHash} already exists: ${existingFile.id}")
                Mono.error(IllegalArgumentException("A file with the same content already exists"))
            }
            .switchIfEmpty(
                Mono.defer {
                    println("Creating new file: ${file.originalName}")
                    // TODO: Aquí necesitaremos los bytes del archivo para subir a MinIO
                    // Por ahora solo guardamos en Db
                    fileRepository.save(file)
                        .map { savedFile ->
                            println("File created successfully: ${savedFile.id}")
                            savedFile as File?
                        }
                }
            )
    }

    override fun findById(id: UUID): Mono<File?> {
        println("=== FILE SERVICE: findById() called for: $id ===")
        
        // Intentar obtener desde cache primero
        return cacheService.getFileMetadata(id.toString())
            .cast(File::class.java)
            .doOnNext { cachedFile -> 
                println("File found in cache: ${cachedFile.id}")
            }
            .switchIfEmpty(
                // Si no está en cache o hay error, buscar en BD y cachear
                fileRepository.findById(id)
                    .cast(File::class.java)
                    .flatMap { file ->
                        println("File found in DB, caching: ${file.id}")
                        // Intentar cachear, pero no fallar si el cache falla
                        cacheService.cacheFileMetadata(id.toString(), file)
                            .doOnSuccess { cached ->
                                if (cached) {
                                    println("File cached successfully: ${file.id}")
                                } else {
                                    println("Failed to cache file: ${file.id}")
                                }
                            }
                            .onErrorResume { error ->
                                println("Cache operation failed for file: ${file.id}. Error: ${error.message}")
                                Mono.just(false)
                            }
                            .thenReturn(file)
                    }
                    .doOnNext { file -> 
                        println("File processing completed: ${file.id}")
                    }
            )
            .map { it as File? }
            .switchIfEmpty(Mono.empty())
            .onErrorResume { error ->
                println("Error in findById for id: $id. Error: ${error.message}")
                // Si todo falla, intentar solo desde BD sin cache
                fileRepository.findById(id)
                    .map { it as File? }
                    .switchIfEmpty(Mono.empty())
            }
    }

    override fun findByOwner(ownerId: UUID): Flux<File> {
        println("=== FILE SERVICE: findByOwner() called for owner: $ownerId ===")
        
        // Intentar obtener desde cache primero
        return cacheService.getUserFiles(ownerId.toString())
            .flatMapMany { cachedFiles: List<File>? ->
                if (cachedFiles != null) {
                    println("Files found in cache for owner: $ownerId (${cachedFiles.size} files)")
                    Flux.fromIterable(cachedFiles)
                } else {
                    Flux.empty()
                }
            }
            .switchIfEmpty(
                // Si no está en cache, buscar en BD y cachear
                fileRepository.findByOwnerId(ownerId)
                    .collectList()
                    .flatMapMany { filesList: List<File> ->
                        println("Files found in DB for owner: $ownerId (${filesList.size} files)")
                        
                        // Cachear la lista de archivos del usuario
                        cacheService.cacheUserFiles(ownerId.toString(), filesList)
                            .doOnSuccess { 
                                println("User files cached successfully for owner: $ownerId")
                            }
                            .doOnError { error ->
                                println("Failed to cache user files for owner: $ownerId. Error: ${error.message}")
                            }
                            .thenMany(Flux.fromIterable(filesList))
                    }
            )
            .doOnNext { file -> println("Found file for owner: ${file.originalName}") }
            .doOnComplete { println("Completed findByOwner") }
            .doOnError { error -> println("Error in findByOwner: ${error.message}") }
    }

    override fun findByOwnerWithPagination(ownerId: UUID, page: Int, size: Int): Flux<File> {
        val offset = (page * size).toLong()
        return fileRepository.findByOwnerIdWithPagination(ownerId, size, offset)
    }

    override fun updateFile(file: File): Mono<File?> {
        val updatedFile = file.copy(updatedAt = LocalDateTime.now())
        return fileRepository.save(updatedFile)
            .doOnSuccess { savedFile ->
                // Invalidar cache de forma asíncrona sin bloquear
                val fileId = savedFile.id
                if (fileId != null) {
                    cacheService.invalidateFileMetadata(fileId.toString())
                        .then(cacheService.invalidateUserFilesCache(savedFile.ownerId.toString()))
                        .doOnSuccess { 
                            println("Cache invalidated for updated file: $fileId")
                        }
                        .subscribe() // Ejecutar de forma asíncrona sin esperar
                }
            }
            .map { it as File? }
    }

    override fun updateFileStatus(id: UUID, status: FileStatus): Mono<File?> {
        return fileRepository.findById(id)
            .flatMap { file ->
                val updatedFile = file.copy(
                    status = status,
                    updatedAt = LocalDateTime.now(),
                    processedAt = if (status == FileStatus.READY || status == FileStatus.FAILED)
                        LocalDateTime.now() else file.processedAt
                )
                fileRepository.save(updatedFile)
                    .doOnSuccess { savedFile ->
                        // Invalidar cache de forma asíncrona sin bloquear
                        val fileId = savedFile.id
                        if (fileId != null) {
                            cacheService.invalidateFileMetadata(fileId.toString())
                                .then(cacheService.invalidateUserFilesCache(savedFile.ownerId.toString()))
                                .doOnSuccess { 
                                    println("Cache invalidated for status update of file: $fileId")
                                }
                                .subscribe() // Ejecutar de forma asíncrona sin esperar
                        }
                    }
            }
            .map { it as File? }
    }

    override fun deleteFile(id: UUID): Mono<Boolean> {
        return fileRepository.findById(id)
            .flatMap { file ->
                // Eliminar de MinIO primero
                minioService.deleteFile(file.storagePath)
                    .flatMap { deleted ->
                        if (deleted) {
                            // Eliminar de BD
                            fileRepository.deleteById(id)
                                .then(
                                    // Invalidar cache del archivo específico y del usuario
                                    Mono.defer {
                                        val fileId = file.id
                                        if (fileId != null) {
                                            cacheService.invalidateFileMetadata(fileId.toString())
                                                .then(cacheService.invalidateUserFilesCache(file.ownerId.toString()))
                                                .then(cacheService.invalidateSearchCache())
                                                .doOnSuccess { 
                                                    println("Cache invalidated for deleted file: $fileId")
                                                }
                                        } else {
                                            Mono.empty<Void>()
                                        }
                                    }
                                )
                                .thenReturn(true)
                        } else {
                            println("Failed to delete file from MinIO: ${file.storagePath}")
                            Mono.just(false)
                        }
                    }
            }
            .onErrorReturn(false)
    }


    /**
     * Upload completo de archivo a MinIO + BD
     */
    fun uploadFileWithContent(
        filename: String,
        originalName: String,
        contentType: String,
        content: Flux<DataBuffer>,
        ownerId: UUID
    ): Mono<File> {
        println("=== FILE SERVICE: uploadFileWithContent() called for: $originalName ===")

        return DataBufferUtils.join(content)
            .map { dataBuffer ->
                val bytes = ByteArray(dataBuffer.readableByteCount())
                dataBuffer.read(bytes)
                DataBufferUtils.release(dataBuffer)
                bytes
            }
            .flatMap { fileBytes ->
                val fileHash = calculateSHA256(fileBytes)
                val storagePath = generateStoragePath(ownerId, filename)

                // Verificar duplicados
                checkFileExists(fileHash)
                    .flatMap { exists ->
                        if (exists) {
                            Mono.error(IllegalArgumentException("File with same content already exists"))
                        } else {
                            // Subir a MinIO
                            val inputStream = ByteArrayInputStream(fileBytes)
                            minioService.uploadFile(inputStream, storagePath, contentType)
                                .flatMap { success ->
                                    if (success) {
                                        // Crear entidad File
                                        val file = File(
                                            id = null,
                                            name = filename,
                                            originalName = originalName,
                                            size = fileBytes.size.toLong(),
                                            contentType = contentType,
                                            fileHash = fileHash,
                                            storagePath = storagePath,
                                            ownerId = ownerId,
                                            status = FileStatus.PENDING
                                        )
                                        // Guardar en BD y publicar evento
                                        fileRepository.save(file)
                                            .flatMap { savedFile ->
                                                println("File uploaded successfully: ${savedFile.id}")
                                                
                                                // Invalidar cache del usuario para que se actualice la lista
                                                cacheService.invalidateUserFilesCache(ownerId.toString())
                                                    .doOnSuccess { 
                                                        println("User files cache invalidated for new upload: ${savedFile.id}")
                                                    }
                                                    .then(
                                                        // Crear y publicar evento
                                                        eventPublisher.publishFileUploadedEvent(
                                                            FileUploadedEvent(
                                                                fileId = savedFile.id!!,
                                                                userId = ownerId,
                                                                fileName = filename,
                                                                originalName = originalName,
                                                                size = fileBytes.size.toLong(),
                                                                contentType = contentType,
                                                                fileHash = fileHash,
                                                                storagePath = storagePath
                                                            )
                                                        )
                                                            .doOnSuccess {
                                                                println("FileUploadedEvent published for file: ${savedFile.id}")
                                                            }
                                                            .doOnError { error ->
                                                                println("Failed to publish FileUploadedEvent for file: ${savedFile.id}. Error: ${error.message}")
                                                            }
                                                    )
                                                    .thenReturn(savedFile)
                                            }
                                    } else {
                                        Mono.error(RuntimeException("Failed to upload file to MinIO"))
                                    }
                                }
                        }
                    }
            }
            .doOnError { error ->
                println("Error uploading file: ${error.message}")
            }
    }

    /**
     * Download de archivo desde MinIO
     */
    fun downloadFileContent(fileId: UUID, userId: UUID): Mono<Pair<File, Flux<DataBuffer>>> {
        return validateFileOwnership(fileId, userId)
            .flatMap { hasAccess ->
                if (hasAccess) {
                    fileRepository.findById(fileId)
                        .flatMap { file ->
                            minioService.downloadFile(file.storagePath)
                                .map { inputStream ->
                                    val dataBufferFlux = DataBufferUtils.readInputStream(
                                        { inputStream },
                                        org.springframework.core.io.buffer.DefaultDataBufferFactory(),
                                        4096
                                    )
                                    Pair(file, dataBufferFlux)
                                }
                        }
                } else {
                    Mono.error(SecurityException("Access denied to file"))
                }
            }
            .doOnNext { (file, _) ->
                println("File downloaded: ${file.id} by user: $userId")
            }
    }

    /**
     * Obtener URL presignada para download
     */
    fun getDownloadUrl(fileId: UUID, userId: UUID, expirationMinutes: Int = 60): Mono<String> {
        return validateFileOwnership(fileId, userId)
            .flatMap { hasAccess ->
                if (hasAccess) {
                    fileRepository.findById(fileId)
                        .flatMap { file ->
                            minioService.generatePresignedDownloadUrl(
                                file.storagePath,
                                java.time.Duration.ofMinutes(expirationMinutes.toLong()) // Convertir a Duration
                            )
                        }
                } else {
                    Mono.error(SecurityException("Access denied to file"))
                }
            }
    }

    // === BUSINESS LOGIC METHODS ===

    override fun findByOwnerAndStatus(ownerId: UUID, status: FileStatus): Flux<File> {
        return fileRepository.findByOwnerIdAndStatus(ownerId, status)
    }

    override fun checkFileExists(fileHash: String): Mono<Boolean> {
        return fileRepository.findByFileHash(fileHash)
            .hasElement()
    }

    override fun findDuplicateFile(fileHash: String): Mono<File?> {
        return fileRepository.findByFileHash(fileHash)
            .map { it as File? }
            .switchIfEmpty(Mono.empty())
    }

    override fun validateFileOwnership(fileId: UUID, ownerId: UUID): Mono<Boolean> {
        return fileRepository.findById(fileId)
            .map { file -> file.ownerId == ownerId }
            .defaultIfEmpty(false)
    }

    // === SEARCH OPERATIONS ===

    override fun searchFilesByName(ownerId: UUID, searchTerm: String): Flux<File> {
        println("=== FILE SERVICE: searchFilesByName() called for owner: $ownerId, term: $searchTerm ===")
        
        val cacheKey = "search:${ownerId}:${searchTerm.lowercase()}"
        
        // Intentar obtener desde cache primero
        return cacheService.getSearchResults(cacheKey)
            .flatMapMany { cachedResults: List<File>? ->
                if (cachedResults != null) {
                    println("Search results found in cache for: $searchTerm (${cachedResults.size} results)")
                    Flux.fromIterable(cachedResults)
                } else {
                    Flux.empty()
                }
            }
            .switchIfEmpty(
                // Si no está en cache, buscar en BD y cachear
                fileRepository.searchByNameAndOwner(ownerId, searchTerm)
                    .collectList()
                    .flatMapMany { resultsList: List<File> ->
                        println("Search results found in DB for: $searchTerm (${resultsList.size} results)")
                        
                        // Cachear los resultados de búsqueda
                        cacheService.cacheSearchResults(cacheKey, resultsList)
                            .doOnSuccess { 
                                println("Search results cached successfully for: $searchTerm")
                            }
                            .doOnError { error ->
                                println("Failed to cache search results for: $searchTerm. Error: ${error.message}")
                            }
                            .thenMany(Flux.fromIterable(resultsList))
                    }
            )
    }

    override fun findFilesByTag(ownerId: UUID, tag: String): Flux<File> {
        return fileRepository.findByOwnerIdAndTagsContaining(ownerId, tag)
    }

    override fun findFilesByContentType(ownerId: UUID, contentTypePrefix: String): Flux<File> {
        return fileRepository.findByOwnerIdAndContentTypeStartingWith(ownerId, contentTypePrefix)
    }

    // === STATISTICS ===

    override fun countFilesByOwner(ownerId: UUID): Mono<Long> {
        return fileRepository.countByOwnerId(ownerId)
    }

    override fun getOwnerStatistics(ownerId: UUID): Mono<Map<String, Any>> {
        return fileRepository.getOwnerStatistics(ownerId)
    }

    // === MAINTENANCE OPERATIONS ===

    override fun findOldFilesByStatus(status: FileStatus, daysOld: Int): Flux<File> {
        return fileRepository.findOldFilesByStatus(status, daysOld)
    }

    override fun cleanupUserFiles(ownerId: UUID): Mono<Void> {
        return fileRepository.deleteByOwnerId(ownerId)
    }

    // ============= MÉTODOS HELPER PRIVADOS =============

    private fun calculateSHA256(bytes: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun generateStoragePath(ownerId: UUID, filename: String): String {
        val sanitizedFilename = filename.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        return "files/$ownerId/${UUID.randomUUID()}_$sanitizedFilename"
    }
}
