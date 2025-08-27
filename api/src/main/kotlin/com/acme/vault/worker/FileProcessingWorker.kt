package com.acme.vault.worker

import com.acme.vault.application.service.EventPublisherService
import com.acme.vault.application.service.FileServiceImpl
import com.acme.vault.domain.events.*
import com.acme.vault.domain.models.FileStatus
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.UUID

/**
 * Worker para procesar eventos de archivos de forma asíncrona
 */
@Component
class FileProcessingWorker(
    private val fileService: FileServiceImpl,
    private val eventPublisher: EventPublisherService
) {

    /**
     * Procesa eventos de archivos (maneja todos los tipos)
     */
    @RabbitListener(queues = ["\${app.rabbitmq.queues.file-processing}"])
    fun handleFileEvent(
        @Payload event: FileEvent,
        @Header("amqp_deliveryTag") deliveryTag: Long
    ) {
        println("=== FILE PROCESSING WORKER: Received ${event.javaClass.simpleName} for file: ${event.fileId} ===")
        
        when (event) {
            is FileUploadedEvent -> {
                println("Processing FileUploadedEvent for file: ${event.fileId}")
                processFileUploadedEvent(event)
                    .doOnSuccess {
                        println("FileUploadedEvent processed successfully for file: ${event.fileId}")
                    }
                    .doOnError { error ->
                        println("Error processing FileUploadedEvent for file: ${event.fileId}. Error: ${error.message}")
                        handleProcessingError(event, error)
                    }
                    .subscribe()
            }
            is FileProcessingCompletedEvent -> {
                println("Received FileProcessingCompletedEvent for file: ${event.fileId} - ignoring (already handled)")
                // No necesitamos procesar este evento aquí, es solo informativo
            }
            is FileProcessingFailedEvent -> {
                println("Received FileProcessingFailedEvent for file: ${event.fileId} - handling error")
                // Aquí podríamos implementar lógica de retry si es necesario
            }
            is FileProcessingStartedEvent -> {
                println("Received FileProcessingStartedEvent for file: ${event.fileId} - ignoring (already handled)")
                // Este evento es solo informativo
            }
            else -> {
                println("Received unknown event type: ${event.javaClass.simpleName} for file: ${event.fileId}")
            }
        }
    }

    /**
     * Procesa un evento de archivo subido
     */
    private fun processFileUploadedEvent(event: FileUploadedEvent): Mono<Void> {
        return Mono.fromRunnable<Void> {
            println("Starting processing for file: ${event.fileId}")
        }
            .then(
                // 1. Cambiar estado a PROCESSING
                updateFileStatus(event.fileId, FileStatus.PROCESSING)
            )
            .then(
                // 2. Publicar evento de inicio de procesamiento
                publishProcessingStartedEvent(event)
            )
            .then(
                // 3. Realizar el procesamiento real
                performFileProcessing(event)
            )
            .then(
                // 4. Cambiar estado a READY
                updateFileStatus(event.fileId, FileStatus.READY)
            )
            .then(
                // 5. Publicar evento de procesamiento completado
                publishProcessingCompletedEvent(event)
            )
            .onErrorResume { error: Throwable ->
                println("Processing failed for file: ${event.fileId}. Error: ${error.message}")
                // En caso de error, cambiar estado a FAILED y publicar evento de falla
                updateFileStatus(event.fileId, FileStatus.FAILED)
                    .then(publishProcessingFailedEvent(event, error))
            }
    }

    /**
     * Actualiza el estado de un archivo
     */
    private fun updateFileStatus(fileId: UUID, status: FileStatus): Mono<Void> {
        return fileService.updateFileStatus(fileId, status)
            .doOnSuccess {
                println("File status updated to $status for file: $fileId")
            }
            .then()
    }

    /**
     * Publica evento de inicio de procesamiento
     */
    private fun publishProcessingStartedEvent(event: FileUploadedEvent): Mono<Void> {
        val startedEvent = FileProcessingStartedEvent(
            fileId = event.fileId,
            userId = event.userId,
            processingType = "BASIC_PROCESSING",
            estimatedDurationSeconds = 30L
        )
        
        return eventPublisher.publishFileProcessingStartedEvent(startedEvent)
    }

    /**
     * Realiza el procesamiento real del archivo
     */
    private fun performFileProcessing(event: FileUploadedEvent): Mono<Void> {
        return Mono.fromRunnable<Void> {
            println("=== PERFORMING FILE PROCESSING for: ${event.fileId} ===")
            
            // Simular procesamiento (validación, análisis, etc.)
            try {
                // 1. Validar integridad del archivo
                validateFileIntegrity(event)
                
                // 2. Extraer metadatos adicionales
                extractMetadata(event)
                
                // 3. Realizar análisis de contenido (si es necesario)
                analyzeContent(event)
                
                // Simular tiempo de procesamiento
                Thread.sleep(2000) // 2 segundos
                
                println("File processing completed for: ${event.fileId}")
            } catch (e: Exception) {
                println("File processing failed for: ${event.fileId}. Error: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Valida la integridad del archivo
     */
    private fun validateFileIntegrity(event: FileUploadedEvent) {
        println("Validating file integrity for: ${event.fileId}")
        // Aquí se podría:
        // - Verificar que el hash coincida
        // - Validar que el archivo no esté corrupto
        // - Verificar el formato del archivo
        
        // Simulación: todo OK
        println("File integrity validation passed for: ${event.fileId}")
    }

    /**
     * Extrae metadatos adicionales del archivo
     */
    private fun extractMetadata(event: FileUploadedEvent) {
        println("Extracting metadata for: ${event.fileId}")
        
        // Aquí se podría:
        // - Extraer metadatos EXIF de imágenes
        // - Obtener duración de videos
        // - Analizar propiedades de documentos
        
        println("Metadata extraction completed for: ${event.fileId}")
    }

    /**
     * Analiza el contenido del archivo
     */
    private fun analyzeContent(event: FileUploadedEvent) {
        println("Analyzing content for: ${event.fileId}")
        
        // Aquí se podría:
        // - Análisis de virus/malware
        // - Clasificación automática
        // - Generación de thumbnails
        // - Compresión automática
        
        println("Content analysis completed for: ${event.fileId}")
    }

    /**
     * Publica evento de procesamiento completado
     */
    private fun publishProcessingCompletedEvent(event: FileUploadedEvent): Mono<Void> {
        val completedEvent = FileProcessingCompletedEvent(
            fileId = event.fileId,
            userId = event.userId,
            processingType = "BASIC_PROCESSING",
            processingDurationSeconds = 2L,
            outputMetadata = mapOf(
                "processed_at" to LocalDateTime.now().toString(),
                "processing_version" to "1.0",
                "file_validated" to true
            )
        )
        
        return eventPublisher.publishFileProcessingCompletedEvent(completedEvent)
    }

    /**
     * Publica evento de procesamiento fallido
     */
    private fun publishProcessingFailedEvent(event: FileUploadedEvent, error: Throwable): Mono<Void> {
        val failedEvent = FileProcessingFailedEvent(
            fileId = event.fileId,
            userId = event.userId,
            processingType = "BASIC_PROCESSING",
            errorMessage = error.message ?: "Unknown error",
            errorCode = error.javaClass.simpleName,
            retryCount = 0,
            canRetry = true
        )
        
        return eventPublisher.publishFileProcessingFailedEvent(failedEvent)
    }

    /**
     * Maneja errores de procesamiento
     */
    private fun handleProcessingError(event: FileUploadedEvent, error: Throwable) {
        println("=== HANDLING PROCESSING ERROR for file: ${event.fileId} ===")
        println("Error: ${error.message}")
        
        // Aquí se podrían implementar estrategias de retry, dead letter queue, etc.
        // Por ahora solo loggeamos el error
    }
}
