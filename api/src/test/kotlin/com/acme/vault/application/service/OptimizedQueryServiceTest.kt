package com.acme.vault.application.service

import com.acme.vault.domain.models.File
import com.acme.vault.domain.models.FileStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.ActiveProfiles
import reactor.test.StepVerifier
import java.time.LocalDateTime
import java.util.*

/**
 * Test de integración para OptimizedQueryService
 * Fase 4.2 - Optimizaciones de BD
 */
@SpringBootTest
@ActiveProfiles("test")
class OptimizedQueryServiceTest {

    @Autowired
    private lateinit var optimizedQueryService: OptimizedQueryService

    @Autowired
    private lateinit var databaseClient: DatabaseClient

    private val testUserId = UUID.randomUUID()

    @Test
    fun `test database functions exist`() {
        // Verificar que las funciones de BD existen
        val functionsQuery = """
            SELECT routine_name 
            FROM information_schema.routines 
            WHERE routine_schema = 'public' 
            AND routine_name IN ('search_files_optimized', 'get_file_stats_by_user', 'refresh_file_stats')
        """.trimIndent()

        StepVerifier.create(
            databaseClient.sql(functionsQuery)
                .fetch()
                .all()
                .collectList()
        )
        .expectNextMatches { functions ->
            println("Funciones encontradas: ${functions.map { it["routine_name"] }}")
            functions.isNotEmpty()
        }
        .verifyComplete()
    }

    @Test
    fun `test database indexes exist`() {
        // Verificar que los índices se crearon
        val indexesQuery = """
            SELECT indexname 
            FROM pg_indexes 
            WHERE tablename = 'file_metadata' 
            AND indexname LIKE 'idx_file_metadata_%'
        """.trimIndent()

        StepVerifier.create(
            databaseClient.sql(indexesQuery)
                .fetch()
                .all()
                .collectList()
        )
        .expectNextMatches { indexes ->
            println("Índices encontrados: ${indexes.map { it["indexname"] }}")
            val indexNames = indexes.map { it["indexname"] as String }
            indexNames.contains("idx_file_metadata_owner_status") &&
            indexNames.contains("idx_file_metadata_owner_created") &&
            indexNames.contains("idx_file_metadata_content_type") &&
            indexNames.contains("idx_file_metadata_ready_files")
        }
        .verifyComplete()
    }

    @Test
    fun `test materialized view exists`() {
        // Verificar que la vista materializada existe
        val viewQuery = """
            SELECT schemaname, matviewname 
            FROM pg_matviews 
            WHERE matviewname = 'file_usage_stats'
        """.trimIndent()

        StepVerifier.create(
            databaseClient.sql(viewQuery)
                .fetch()
                .all()
                .collectList()
        )
        .expectNextMatches { views ->
            println("Vistas materializadas encontradas: ${views.map { it["matviewname"] }}")
            views.isNotEmpty()
        }
        .verifyComplete()
    }

    @Test
    fun `test optimized query service methods`() {
        // Insertar datos de prueba
        val insertQuery = """
            INSERT INTO file_metadata (
                id, name, original_name, size, content_type, file_hash, 
                storage_path, owner_id, status, tags, description, 
                created_at, updated_at
            ) VALUES (
                :id, :name, :originalName, :size, :contentType, :fileHash,
                :storagePath, :ownerId, :status, :tags, :description,
                :createdAt, :updatedAt
            )
        """.trimIndent()

        val testFile = mapOf(
            "id" to UUID.randomUUID(),
            "name" to "test-file.pdf",
            "originalName" to "Test Document.pdf",
            "size" to 1024L,
            "contentType" to "application/pdf",
            "fileHash" to "test-hash-123",
            "storagePath" to "/storage/test-file.pdf",
            "ownerId" to testUserId,
            "status" to "READY",
            "tags" to arrayOf("test", "document"),
            "description" to "Test document for optimization",
            "createdAt" to LocalDateTime.now(),
            "updatedAt" to LocalDateTime.now()
        )

        StepVerifier.create(
            databaseClient.sql(insertQuery)
                .bindValues(testFile)
                .fetch()
                .rowsUpdated()
                .then(
                    // Ahora probar las búsquedas optimizadas
                    optimizedQueryService.findFilesByContentTypeOptimized(
                        testUserId, "application", 10
                    ).collectList()
                )
        )
        .expectNextMatches { files ->
            println("Archivos encontrados: ${files.size}")
            files.isNotEmpty()
        }
        .verifyComplete()
    }

    @Test
    fun `test performance metrics`() {
        // Probar métricas de rendimiento
        StepVerifier.create(
            optimizedQueryService.getQueryPerformanceMetrics()
        )
        .expectNextMatches { metrics ->
            println("Métricas obtenidas: $metrics")
            metrics.containsKey("tableStats") && 
            metrics.containsKey("timestamp") &&
            metrics.containsKey("totalRows")
        }
        .verifyComplete()
    }

    @Test
    fun `test connection pool configuration`() {
        // Verificar configuración del pool de conexiones
        val poolQuery = """
            SELECT 
                numbackends as active_connections,
                numbackends as total_connections
            FROM pg_stat_database 
            WHERE datname = 'management'
        """.trimIndent()

        StepVerifier.create(
            databaseClient.sql(poolQuery)
                .fetch()
                .one()
        )
        .expectNextMatches { stats ->
            println("Estadísticas de conexión: $stats")
            stats.containsKey("active_connections")
        }
        .verifyComplete()
    }
}
