package com.acme.vault.worker

import com.acme.vault.application.service.EventPublisherService
import com.acme.vault.application.service.FileServiceImpl
import com.acme.vault.domain.events.*
import com.acme.vault.domain.models.FileStatus
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.UUID

@Component
class FileProcessingWorker(
    private val fileService: FileServiceImpl,
    private val eventPublisher: EventPublisherService
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(FileProcessingWorker::class.java)
        private const val PROCESSING_TYPE = "BASIC_PROCESSING"
        private const val PROCESSING_VERSION = "1.0"
        private const val ESTIMATED_DURATION_SECONDS = 30L
        private const val SIMULATED_PROCESSING_TIME_MS = 2000L
    }

    @RabbitListener(queues = ["\${app.rabbitmq.queues.file-processing}"])
    fun handleFileEvent(
        @Payload event: FileEvent,
        @Header("amqp_deliveryTag") deliveryTag: Long
    ) {
        logger.info("Received {} for file: {}", event.javaClass.simpleName, event.fileId)
        
        when (event) {
            is FileUploadedEvent -> processFileUploadedEvent(event)
                .doOnSuccess { logger.info("Successfully processed upload for file: {}", event.fileId) }
                .doOnError { error -> handleProcessingError(event, error) }
                .subscribe()
            
            is FileProcessingCompletedEvent, 
            is FileProcessingStartedEvent -> logger.debug("Ignoring informational event for file: {}", event.fileId)
            
            is FileProcessingFailedEvent -> logger.warn("Received failure event for file: {}", event.fileId)
            
            else -> logger.warn("Unknown event type: {} for file: {}", event.javaClass.simpleName, event.fileId)
        }
    }

    private fun processFileUploadedEvent(event: FileUploadedEvent): Mono<Void> {
        return updateFileStatus(event.fileId, FileStatus.PROCESSING)
            .then(publishProcessingStartedEvent(event))
            .then(performFileProcessing(event))
            .then(updateFileStatus(event.fileId, FileStatus.READY))
            .then(publishProcessingCompletedEvent(event))
            .onErrorResume { error ->
                logger.error("Processing failed for file: {}", event.fileId, error)
                updateFileStatus(event.fileId, FileStatus.FAILED)
                    .then(publishProcessingFailedEvent(event, error))
            }
    }

    private fun updateFileStatus(fileId: UUID, status: FileStatus): Mono<Void> {
        return fileService.updateFileStatus(fileId, status)
            .doOnSuccess { logger.debug("Updated file {} status to {}", fileId, status) }
            .then()
    }

    private fun publishProcessingStartedEvent(event: FileUploadedEvent): Mono<Void> {
        val startedEvent = FileProcessingStartedEvent(
            fileId = event.fileId,
            userId = event.userId,
            processingType = PROCESSING_TYPE,
            estimatedDurationSeconds = ESTIMATED_DURATION_SECONDS
        )
        return eventPublisher.publishFileProcessingStartedEvent(startedEvent)
    }

    private fun performFileProcessing(event: FileUploadedEvent): Mono<Void> {
        return Mono.fromRunnable<Void> {
            logger.info("Processing file: {}", event.fileId)
            
            runCatching {
                validateFileIntegrity(event)
                extractMetadata(event)
                analyzeContent(event)
                simulateProcessingTime()
            }.onFailure { error ->
                logger.error("Processing pipeline failed for file: {}", event.fileId, error)
                throw error
            }
            
            logger.info("Processing completed for file: {}", event.fileId)
        }
    }

    private fun validateFileIntegrity(event: FileUploadedEvent) {
        logger.debug("Validating integrity for file: {}", event.fileId)
    }

    private fun extractMetadata(event: FileUploadedEvent) {
        logger.debug("Extracting metadata for file: {}", event.fileId)
    }

    private fun analyzeContent(event: FileUploadedEvent) {
        logger.debug("Analyzing content for file: {}", event.fileId)
    }

    private fun simulateProcessingTime() {
        Thread.sleep(SIMULATED_PROCESSING_TIME_MS)
    }

    private fun publishProcessingCompletedEvent(event: FileUploadedEvent): Mono<Void> {
        val completedEvent = FileProcessingCompletedEvent(
            fileId = event.fileId,
            userId = event.userId,
            processingType = PROCESSING_TYPE,
            processingDurationSeconds = SIMULATED_PROCESSING_TIME_MS / 1000,
            outputMetadata = mapOf(
                "processed_at" to LocalDateTime.now().toString(),
                "processing_version" to PROCESSING_VERSION,
                "file_validated" to true
            )
        )
        return eventPublisher.publishFileProcessingCompletedEvent(completedEvent)
    }

    private fun publishProcessingFailedEvent(event: FileUploadedEvent, error: Throwable): Mono<Void> {
        val failedEvent = FileProcessingFailedEvent(
            fileId = event.fileId,
            userId = event.userId,
            processingType = PROCESSING_TYPE,
            errorMessage = error.message ?: "Unknown processing error",
            errorCode = error.javaClass.simpleName,
            retryCount = 0,
            canRetry = true
        )
        return eventPublisher.publishFileProcessingFailedEvent(failedEvent)
    }

    private fun handleProcessingError(event: FileUploadedEvent, error: Throwable) {
        logger.error("Processing error for file: {} - {}", event.fileId, error.message)
    }
}
