package com.acme.vault.domain.service

import com.acme.vault.domain.models.File
import com.acme.vault.domain.models.FileStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface IFileService {
    
    // CRUD Operations
    fun createFile(file: File): Mono<File?>
    fun findById(id: UUID): Mono<File?>
    fun findByOwner(ownerId: UUID): Flux<File>
    fun findByOwnerWithPagination(ownerId: UUID, page: Int, size: Int): Flux<File>
    fun updateFile(file: File): Mono<File?>
    fun updateFileStatus(id: UUID, status: FileStatus): Mono<File?>
    fun deleteFile(id: UUID): Mono<Boolean>
    
    // Business Logic
    fun findByOwnerAndStatus(ownerId: UUID, status: FileStatus): Flux<File>
    fun checkFileExists(fileHash: String): Mono<Boolean>
    fun findDuplicateFile(fileHash: String): Mono<File?>
    fun validateFileOwnership(fileId: UUID, ownerId: UUID): Mono<Boolean>
    
    // Search Operations
    fun searchFilesByName(ownerId: UUID, searchTerm: String): Flux<File>
    fun findFilesByTag(ownerId: UUID, tag: String): Flux<File>
    fun findFilesByContentType(ownerId: UUID, contentTypePrefix: String): Flux<File>
    
    // Statistics
    fun countFilesByOwner(ownerId: UUID): Mono<Long>
    fun getOwnerStatistics(ownerId: UUID): Mono<Map<String, Any>>
    
    // Maintenance Operations
    fun findOldFilesByStatus(status: FileStatus, daysOld: Int): Flux<File>
    fun cleanupUserFiles(ownerId: UUID): Mono<Void>
}
