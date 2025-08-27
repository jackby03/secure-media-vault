package com.acme.vault.adapter.web

import com.acme.vault.adapter.web.dto.FileListResponse
import com.acme.vault.adapter.web.dto.FileResponse
import com.acme.vault.adapter.web.mapper.FileMapper
import com.acme.vault.application.service.FileServiceImpl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/api/files")
class FileController(
    private val fileService: FileServiceImpl,
    private val fileMapper: FileMapper
) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    fun uploadFile(
        @RequestPart("file") filePart: Mono<FilePart>,
        authentication: Authentication
    ): Mono<ResponseEntity<FileResponse>> {

        return filePart.flatMap { file ->
            val userId = extractUserId(authentication)
            val filename = file.filename()
            val originalName = filename
            val contentType = "application/octet-stream" // Default

            fileService.uploadFileWithContent(
                filename = filename,
                originalName = originalName,
                contentType = contentType,
                content = file.content(),
                ownerId = userId
            )
        }
            .map { uploadedFile ->
                ResponseEntity.status(HttpStatus.CREATED)
                    .body(fileMapper.toFileResponse(uploadedFile))
            }
            .onErrorMap { error ->
                when (error) {
                    is IllegalArgumentException -> ResponseStatusException(HttpStatus.BAD_REQUEST, error.message)
                    else -> ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload failed")
                }
            }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    fun listFiles(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        authentication: Authentication
    ): Mono<ResponseEntity<FileListResponse>> {

        val userId = extractUserId(authentication)

        return fileService.findByOwnerWithPagination(userId, page, size)
            .collectList()
            .zipWith(fileService.countFilesByOwner(userId))
            .map { tuple ->
                val files = tuple.t1
                val totalElements = tuple.t2
                val response = fileMapper.toFileListResponse(
                    files = files,
                    totalElements = totalElements,
                    currentPage = page,
                    pageSize = size
                )
                ResponseEntity.ok(response)
            }
            .onErrorMap { unused ->
                ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to list files")
            }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    fun getFileMetadata(
        @PathVariable id: UUID,
        authentication: Authentication
    ): Mono<ResponseEntity<FileResponse>> {

        val userId = extractUserId(authentication)

        return fileService.validateFileOwnership(id, userId)
            .flatMap { hasAccess ->
                if (hasAccess) {
                    fileService.findById(id)
                        .map { file ->
                            if (file != null) {
                                ResponseEntity.ok(fileMapper.toFileResponse(file))
                            } else {
                                ResponseEntity.notFound().build<FileResponse>()
                            }
                        }
                } else {
                    Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build<FileResponse>())
                }
            }
            .onErrorMap { unused ->
                ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get file metadata")
            }
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    fun downloadFile(
        @PathVariable id: UUID,
        authentication: Authentication
    ): Mono<ResponseEntity<String>> {

        val userId = extractUserId(authentication)

        return fileService.getDownloadUrl(id, userId, 60)
            .map { downloadUrl ->
                ResponseEntity.ok(downloadUrl)
            }
            .onErrorMap { error ->
                when (error) {
                    is SecurityException -> ResponseStatusException(HttpStatus.FORBIDDEN, error.message)
                    else -> ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Download failed")
                }
            }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    fun deleteFile(
        @PathVariable id: UUID,
        authentication: Authentication
    ): Mono<ResponseEntity<Void>> {

        val userId = extractUserId(authentication)

        return fileService.validateFileOwnership(id, userId)
            .flatMap { hasAccess ->
                if (hasAccess) {
                    fileService.deleteFile(id)
                        .map { deleted ->
                            if (deleted) {
                                ResponseEntity.noContent().build<Void>()
                            } else {
                                ResponseEntity.notFound().build<Void>()
                            }
                        }
                } else {
                    Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build<Void>())
                }
            }
            .onErrorMap { unused ->
                ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Delete failed")
            }
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    fun searchFiles(
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        authentication: Authentication
    ): Mono<ResponseEntity<FileListResponse>> {

        val userId = extractUserId(authentication)

        return fileService.searchFilesByName(userId, query)
            .skip((page * size).toLong())
            .take(size.toLong())
            .collectList()
            .zipWith(fileService.searchFilesByName(userId, query).count())
            .map { tuple ->
                val files = tuple.t1
                val totalElements = tuple.t2
                val response = fileMapper.toFileListResponse(
                    files = files,
                    totalElements = totalElements,
                    currentPage = page,
                    pageSize = size
                )
                ResponseEntity.ok(response)
            }
            .onErrorMap { unused ->
                ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Search failed")
            }
    }

    // Helper method - simple y sin validaciones extras
    private fun extractUserId(authentication: Authentication): UUID {
        // Por ahora uso un UUID fijo hasta que tengamos la autenticación completa
        // En producción esto debe extraerse del JWT
        return UUID.fromString("123e4567-e89b-12d3-a456-426614174000") // UUID temporal
    }
}