package com.acme.vault.application.service

import com.acme.vault.adapter.persistance.FileRepository
import com.acme.vault.domain.models.File
import com.acme.vault.domain.models.FileStatus
import com.acme.vault.domain.service.IFileService
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.UUID

@Service
class FileServiceImpl(
    private val fileRepository: FileRepository
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
                    fileRepository.save(file)
                        .map { savedFile ->
                            println("File created successfully: ${savedFile.id}")
                            savedFile as File?
                        }
                }
            )
    }

    override fun findById(id: UUID): Mono<File?> {
        return fileRepository.findById(id)
            .map { it as File? }
            .switchIfEmpty(Mono.empty())
    }

    override fun findByOwner(ownerId: UUID): Flux<File> {
        println("=== FILE SERVICE: findByOwner() called for owner: $ownerId ===")
        return fileRepository.findByOwnerId(ownerId)
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
            }
            .map { it as File? }
    }

    override fun deleteFile(id: UUID): Mono<Boolean> {
        return fileRepository.deleteById(id)
            .thenReturn(true)
            .onErrorReturn(false)
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
        return fileRepository.searchByNameAndOwner(ownerId, searchTerm)
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
}
