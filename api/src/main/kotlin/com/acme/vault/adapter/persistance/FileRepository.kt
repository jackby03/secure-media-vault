package com.acme.vault.adapter.persistance

import com.acme.vault.domain.models.File
import com.acme.vault.domain.models.FileStatus
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
interface FileRepository : R2dbcRepository<File, UUID> {
    
    fun findByOwnerId(ownerId: UUID): Flux<File>
    
    fun findByOwnerIdAndStatus(ownerId: UUID, status: FileStatus): Flux<File>
    
    fun findByFileHash(fileHash: String): Mono<File>
    
    @Query("""
        SELECT * FROM file_metadata 
        WHERE owner_id = :ownerId 
        ORDER BY created_at DESC 
        LIMIT :limit OFFSET :offset
    """)
    fun findByOwnerIdWithPagination(ownerId: UUID, limit: Int, offset: Long): Flux<File>
    
    fun countByOwnerId(ownerId: UUID): Mono<Long>
    
    fun findByOwnerIdAndContentTypeStartingWith(ownerId: UUID, contentTypePrefix: String): Flux<File>
    
    @Query("""
        SELECT * FROM file_metadata 
        WHERE owner_id = :ownerId 
        AND to_tsvector('english', name) @@ plainto_tsquery('english', :searchTerm)
    """)
    fun searchByNameAndOwner(ownerId: UUID, searchTerm: String): Flux<File>
    
    @Query("""
        SELECT * FROM file_metadata 
        WHERE owner_id = :ownerId 
        AND tags && ARRAY[:tag]::text[]
    """)
    fun findByOwnerIdAndTagsContaining(ownerId: UUID, tag: String): Flux<File>
    
    @Query("""
        SELECT 
            COUNT(*) as total_files,
            SUM(size) as total_size,
            COUNT(CASE WHEN status = 'READY' THEN 1 END) as ready_files,
            COUNT(CASE WHEN status = 'PROCESSING' THEN 1 END) as processing_files,
            COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed_files
        FROM file_metadata 
        WHERE owner_id = :ownerId
    """)
    fun getOwnerStatistics(ownerId: UUID): Mono<Map<String, Any>>
    
    fun deleteByOwnerId(ownerId: UUID): Mono<Void>
    
    @Query("""
        SELECT * FROM file_metadata 
        WHERE status = :status 
        AND created_at < NOW() - INTERVAL ':daysOld days'
    """)
    fun findOldFilesByStatus(status: FileStatus, daysOld: Int): Flux<File>
}
