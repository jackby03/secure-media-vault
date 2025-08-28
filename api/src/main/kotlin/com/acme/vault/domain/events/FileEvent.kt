package com.acme.vault.domain.events

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDateTime
import java.util.UUID

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventType"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = FileUploadedEvent::class, name = "FILE_UPLOADED"),
    JsonSubTypes.Type(value = FileProcessingStartedEvent::class, name = "FILE_PROCESSING_STARTED"),
    JsonSubTypes.Type(value = FileProcessingCompletedEvent::class, name = "FILE_PROCESSING_COMPLETED"),
    JsonSubTypes.Type(value = FileProcessingFailedEvent::class, name = "FILE_PROCESSING_FAILED")
)
abstract class FileEvent {
    abstract val eventId: UUID
    abstract val fileId: UUID
    abstract val userId: UUID
    abstract val timestamp: LocalDateTime
    abstract val eventType: String
}

data class FileUploadedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val fileId: UUID,
    override val userId: UUID,
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val eventType: String = "FILE_UPLOADED",
    val fileName: String,
    val originalName: String,
    val size: Long,
    val contentType: String,
    val fileHash: String,
    val storagePath: String
) : FileEvent()

data class FileProcessingStartedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val fileId: UUID,
    override val userId: UUID,
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val eventType: String = "FILE_PROCESSING_STARTED",
    val processingType: String,
    val estimatedDurationSeconds: Long? = null
) : FileEvent()

data class FileProcessingCompletedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val fileId: UUID,
    override val userId: UUID,
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val eventType: String = "FILE_PROCESSING_COMPLETED",
    val processingType: String,
    val processingDurationSeconds: Long,
    val outputMetadata: Map<String, Any> = emptyMap()
) : FileEvent()

data class FileProcessingFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val fileId: UUID,
    override val userId: UUID,
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val eventType: String = "FILE_PROCESSING_FAILED",
    val processingType: String,
    val errorMessage: String,
    val errorCode: String? = null,
    val retryCount: Int = 0,
    val canRetry: Boolean = false
) : FileEvent()
