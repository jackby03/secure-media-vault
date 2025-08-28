package com.acme.vault.application.service

import com.acme.vault.domain.models.File
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.UUID

/**
 * Servicio de consultas optimizadas para la base de datos
 * Fase 4.2 - Optimizaciones de BD
 */
@Service
class OptimizedQueryService(
    @Autowired private val databaseClient: DatabaseClient
) {

    /**
     * Búsqueda optimizada de archivos con full-text search y filtros
     */
    fun searchFilesOptimized(
        userId: UUID,
        searchTerm: String? = null,
        contentTypeFilter: String? = null,
        statusFilter: String = "READY",
        limit: Int = 20,
        offset: Int = 0
    ): Flux<File> {
        return databaseClient
            .sql("""
                SELECT * FROM search_files_optimized(
                    :userId, :searchTerm, :contentTypeFilter, :statusFilter, :limit, :offset
                )
            """.trimIndent())
            .bind("userId", userId)
            .bind("searchTerm", searchTerm ?: "")
            .bind("contentTypeFilter", contentTypeFilter ?: "")
            .bind("statusFilter", statusFilter)
            .bind("limit", limit)
            .bind("offset", offset)
            .fetch()
            .all()
            .map { row ->
                File(
                    id = row["id"] as UUID,
                    name = row["name"] as String,
                    originalName = row["original_name"] as String,
                    size = row["size"] as Long,
                    contentType = row["content_type"] as String,
                    fileHash = "", // No needed for search results
                    storagePath = "", // Not included in search function
                    ownerId = userId,
                    status = com.acme.vault.domain.models.FileStatus.valueOf(statusFilter),
                    tags = emptyList(),
                    description = null,
                    createdAt = row["created_at"] as LocalDateTime,
                    updatedAt = LocalDateTime.now(),
                    processedAt = null
                )
            }
    }

    /**
     * Obtiene estadísticas de archivos por usuario usando función optimizada
     */
    fun getFileStatsByUser(userId: UUID): Mono<Map<String, Any>> {
        return databaseClient
            .sql("SELECT * FROM get_file_stats_by_user(:userId)")
            .bind("userId", userId)
            .fetch()
            .one()
            .map { row ->
                mapOf(
                    "totalFiles" to (row["total_files"] as Long),
                    "totalSize" to (row["total_size"] as Long),
                    "readyFiles" to (row["ready_files"] as Long),
                    "processingFiles" to (row["processing_files"] as Long),
                    "failedFiles" to (row["failed_files"] as Long),
                    "avgFileSize" to (row["avg_file_size"] as Number).toDouble(),
                    "mostCommonType" to (row["most_common_type"] as String? ?: "unknown")
                )
            }
    }

    /**
     * Búsqueda de archivos por rango de fechas optimizada
     */
    fun findFilesByDateRange(
        userId: UUID,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        limit: Int = 50
    ): Flux<File> {
        return databaseClient
            .sql("""
                SELECT id, name, original_name, size, content_type, file_hash, 
                       storage_path, owner_id, status, tags, description, 
                       created_at, updated_at, processed_at
                FROM file_metadata 
                WHERE owner_id = :userId 
                  AND status = 'READY'
                  AND created_at BETWEEN :startDate AND :endDate
                ORDER BY created_at DESC
                LIMIT :limit
            """.trimIndent())
            .bind("userId", userId)
            .bind("startDate", startDate)
            .bind("endDate", endDate)
            .bind("limit", limit)
            .fetch()
            .all()
            .map(this::mapRowToFile)
    }

    /**
     * Búsqueda de archivos por rango de tamaño
     */
    fun findFilesBySizeRange(
        userId: UUID,
        minSize: Long,
        maxSize: Long,
        limit: Int = 50
    ): Flux<File> {
        return databaseClient
            .sql("""
                SELECT id, name, original_name, size, content_type, file_hash, 
                       storage_path, owner_id, status, tags, description, 
                       created_at, updated_at, processed_at
                FROM file_metadata 
                WHERE owner_id = :userId 
                  AND status = 'READY'
                  AND size BETWEEN :minSize AND :maxSize
                ORDER BY size DESC
                LIMIT :limit
            """.trimIndent())
            .bind("userId", userId)
            .bind("minSize", minSize)
            .bind("maxSize", maxSize)
            .bind("limit", limit)
            .fetch()
            .all()
            .map(this::mapRowToFile)
    }

    /**
     * Búsqueda de archivos por tipo de contenido optimizada
     */
    fun findFilesByContentTypeOptimized(
        userId: UUID,
        contentTypePrefix: String,
        limit: Int = 50
    ): Flux<File> {
        val indexHint = when (contentTypePrefix.lowercase()) {
            "image" -> "idx_file_metadata_images"
            "video" -> "idx_file_metadata_videos"
            "application/pdf", "application/msword" -> "idx_file_metadata_documents"
            else -> null
        }

        val sql = if (indexHint != null) {
            """
            SELECT id, name, original_name, size, content_type, file_hash, 
                   storage_path, owner_id, status, tags, description, 
                   created_at, updated_at, processed_at
            FROM file_metadata 
            WHERE owner_id = :userId 
              AND content_type LIKE :contentType
              AND status = 'READY'
            ORDER BY created_at DESC
            LIMIT :limit
            """.trimIndent()
        } else {
            """
            SELECT id, name, original_name, size, content_type, file_hash, 
                   storage_path, owner_id, status, tags, description, 
                   created_at, updated_at, processed_at
            FROM file_metadata 
            WHERE owner_id = :userId 
              AND content_type LIKE :contentType
              AND status = 'READY'
            ORDER BY created_at DESC
            LIMIT :limit
            """.trimIndent()
        }

        return databaseClient
            .sql(sql)
            .bind("userId", userId)
            .bind("contentType", "$contentTypePrefix%")
            .bind("limit", limit)
            .fetch()
            .all()
            .map(this::mapRowToFile)
    }

    /**
     * Operación batch para actualizar múltiples archivos
     */
    fun batchUpdateFileStatus(fileIds: List<UUID>, newStatus: String): Mono<Long> {
        return databaseClient
            .sql("""
                UPDATE file_metadata 
                SET status = :status, updated_at = CURRENT_TIMESTAMP
                WHERE id = ANY(:fileIds)
            """.trimIndent())
            .bind("status", newStatus)
            .bind("fileIds", fileIds.toTypedArray())
            .fetch()
            .rowsUpdated()
    }

    /**
     * Operación batch para eliminar archivos antiguos
     */
    fun batchDeleteOldFiles(userId: UUID, olderThanDays: Int): Mono<Long> {
        return databaseClient
            .sql("""
                DELETE FROM file_metadata 
                WHERE owner_id = :userId 
                  AND status = 'FAILED'
                  AND created_at < CURRENT_TIMESTAMP - INTERVAL ':days days'
            """.trimIndent())
            .bind("userId", userId)
            .bind("days", olderThanDays)
            .fetch()
            .rowsUpdated()
    }

    /**
     * Refresca las estadísticas materializadas
     */
    fun refreshMaterializedStats(): Mono<Void> {
        return databaseClient
            .sql("SELECT refresh_file_stats()")
            .fetch()
            .rowsUpdated()
            .then()
    }

    /**
     * Obtiene métricas de rendimiento de consultas
     */
    fun getQueryPerformanceMetrics(): Mono<Map<String, Any>> {
        return databaseClient
            .sql("""
                SELECT 
                    schemaname,
                    tablename,
                    attname,
                    inherited,
                    null_frac,
                    avg_width,
                    n_distinct,
                    most_common_vals,
                    most_common_freqs,
                    histogram_bounds
                FROM pg_stats 
                WHERE tablename = 'file_metadata'
                ORDER BY attname
            """.trimIndent())
            .fetch()
            .all()
            .collectList()
            .map { rows ->
                mapOf(
                    "tableStats" to rows,
                    "timestamp" to LocalDateTime.now(),
                    "totalRows" to rows.size
                )
            }
    }

    /**
     * Mapea una fila de la BD a objeto File
     */
    @Suppress("UNCHECKED_CAST")
    private fun mapRowToFile(row: Map<String, Any>): File {
        return File(
            id = row["id"] as UUID,
            name = row["name"] as String,
            originalName = row["original_name"] as String,
            size = row["size"] as Long,
            contentType = row["content_type"] as String,
            fileHash = row["file_hash"] as String,
            storagePath = row["storage_path"] as String,
            ownerId = row["owner_id"] as UUID,
            status = com.acme.vault.domain.models.FileStatus.valueOf(row["status"] as String),
            tags = (row["tags"] as? Array<String>)?.toList() ?: emptyList(),
            description = row["description"] as String?,
            createdAt = row["created_at"] as LocalDateTime,
            updatedAt = row["updated_at"] as LocalDateTime,
            processedAt = row["processed_at"] as LocalDateTime?
        )
    }
}
