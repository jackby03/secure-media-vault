package com.acme.vault.domain.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@DisplayName("FileEvent Domain Tests")
class FileEventTest {

    private lateinit var objectMapper: ObjectMapper
    private val testFileId = UUID.randomUUID()
    private val testUserId = UUID.randomUUID()
    private val testTimestamp = LocalDateTime.now()

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
            registerModule(JavaTimeModule())
        }
    }

    @Nested
    @DisplayName("FileUploadedEvent Tests")
    inner class FileUploadedEventTests {

        @Test
        fun `should create FileUploadedEvent with all required properties`() {
            val event = FileUploadedEvent(
                fileId = testFileId,
                userId = testUserId,
                timestamp = testTimestamp,
                fileName = "test-file.pdf",
                originalName = "Test Document.pdf",
                size = 1024L,
                contentType = "application/pdf",
                fileHash = "abc123def456",
                storagePath = "users/$testUserId/test-file.pdf"
            )

            assertEquals(testFileId, event.fileId)
            assertEquals(testUserId, event.userId)
            assertEquals(testTimestamp, event.timestamp)
            assertEquals("FILE_UPLOADED", event.eventType)
            assertEquals("test-file.pdf", event.fileName)
            assertEquals("Test Document.pdf", event.originalName)
            assertEquals(1024L, event.size)
            assertEquals("application/pdf", event.contentType)
            assertEquals("abc123def456", event.fileHash)
            assertEquals("users/$testUserId/test-file.pdf", event.storagePath)
            assertNotNull(event.eventId)
        }

        @Test
        fun `should generate unique event IDs for multiple instances`() {
            val event1 = FileUploadedEvent(
                fileId = testFileId,
                userId = testUserId,
                fileName = "file1.pdf",
                originalName = "file1.pdf",
                size = 1024L,
                contentType = "application/pdf",
                fileHash = "hash1",
                storagePath = "path1"
            )

            val event2 = FileUploadedEvent(
                fileId = testFileId,
                userId = testUserId,
                fileName = "file2.pdf",
                originalName = "file2.pdf",
                size = 2048L,
                contentType = "application/pdf",
                fileHash = "hash2",
                storagePath = "path2"
            )

            assertNotEquals(event1.eventId, event2.eventId)
        }

        @Test
        fun `should use current timestamp by default`() {
            val beforeCreation = LocalDateTime.now()
            val event = FileUploadedEvent(
                fileId = testFileId,
                userId = testUserId,
                fileName = "test.pdf",
                originalName = "test.pdf",
                size = 1024L,
                contentType = "application/pdf",
                fileHash = "hash",
                storagePath = "path"
            )
            val afterCreation = LocalDateTime.now()

            assertTrue(event.timestamp.isAfter(beforeCreation.minus(1, ChronoUnit.SECONDS)))
            assertTrue(event.timestamp.isBefore(afterCreation.plus(1, ChronoUnit.SECONDS)))
        }

        @Test
        fun `should handle large file sizes`() {
            val largeSize = 5L * 1024 * 1024 * 1024 // 5GB
            val event = FileUploadedEvent(
                fileId = testFileId,
                userId = testUserId,
                fileName = "large-file.zip",
                originalName = "large-file.zip",
                size = largeSize,
                contentType = "application/zip",
                fileHash = "largefile-hash",
                storagePath = "users/$testUserId/large-file.zip"
            )

            assertEquals(largeSize, event.size)
        }

        @Test
        fun `should handle special characters in file names`() {
            val specialName = "Tëst Fílé with (spéciál) chärs & símböls.pdf"
            val event = FileUploadedEvent(
                fileId = testFileId,
                userId = testUserId,
                fileName = "sanitized-file.pdf",
                originalName = specialName,
                size = 1024L,
                contentType = "application/pdf",
                fileHash = "hash",
                storagePath = "path"
            )

            assertEquals(specialName, event.originalName)
            assertEquals("sanitized-file.pdf", event.fileName)
        }
    }

    @Nested
    @DisplayName("FileProcessingStartedEvent Tests")
    inner class FileProcessingStartedEventTests {

        @Test
        fun `should create FileProcessingStartedEvent with required properties`() {
            val event = FileProcessingStartedEvent(
                fileId = testFileId,
                userId = testUserId,
                timestamp = testTimestamp,
                processingType = "THUMBNAIL_GENERATION",
                estimatedDurationSeconds = 30L
            )

            assertEquals(testFileId, event.fileId)
            assertEquals(testUserId, event.userId)
            assertEquals(testTimestamp, event.timestamp)
            assertEquals("FILE_PROCESSING_STARTED", event.eventType)
            assertEquals("THUMBNAIL_GENERATION", event.processingType)
            assertEquals(30L, event.estimatedDurationSeconds)
            assertNotNull(event.eventId)
        }

        @Test
        fun `should handle null estimated duration`() {
            val event = FileProcessingStartedEvent(
                fileId = testFileId,
                userId = testUserId,
                processingType = "VIRUS_SCAN"
            )

            assertEquals("VIRUS_SCAN", event.processingType)
            assertNull(event.estimatedDurationSeconds)
        }

        @ParameterizedTest
        @ValueSource(strings = [
            "THUMBNAIL_GENERATION",
            "VIRUS_SCAN", 
            "METADATA_EXTRACTION",
            "CONTENT_ANALYSIS",
            "PREVIEW_GENERATION"
        ])
        fun `should support different processing types`(processingType: String) {
            val event = FileProcessingStartedEvent(
                fileId = testFileId,
                userId = testUserId,
                processingType = processingType
            )

            assertEquals(processingType, event.processingType)
            assertEquals("FILE_PROCESSING_STARTED", event.eventType)
        }

        @Test
        fun `should handle very long estimated durations`() {
            val longDuration = 3600L // 1 hour
            val event = FileProcessingStartedEvent(
                fileId = testFileId,
                userId = testUserId,
                processingType = "VIDEO_ENCODING",
                estimatedDurationSeconds = longDuration
            )

            assertEquals(longDuration, event.estimatedDurationSeconds)
        }
    }

    @Nested
    @DisplayName("FileProcessingCompletedEvent Tests")
    inner class FileProcessingCompletedEventTests {

        @Test
        fun `should create FileProcessingCompletedEvent with required properties`() {
            val metadata = mapOf(
                "thumbnailPath" to "/path/to/thumbnail.jpg",
                "width" to 1920,
                "height" to 1080,
                "duration" to 120.5
            )

            val event = FileProcessingCompletedEvent(
                fileId = testFileId,
                userId = testUserId,
                timestamp = testTimestamp,
                processingType = "THUMBNAIL_GENERATION",
                processingDurationSeconds = 25L,
                outputMetadata = metadata
            )

            assertEquals(testFileId, event.fileId)
            assertEquals(testUserId, event.userId)
            assertEquals(testTimestamp, event.timestamp)
            assertEquals("FILE_PROCESSING_COMPLETED", event.eventType)
            assertEquals("THUMBNAIL_GENERATION", event.processingType)
            assertEquals(25L, event.processingDurationSeconds)
            assertEquals(metadata, event.outputMetadata)
            assertNotNull(event.eventId)
        }

        @Test
        fun `should handle empty output metadata`() {
            val event = FileProcessingCompletedEvent(
                fileId = testFileId,
                userId = testUserId,
                processingType = "VIRUS_SCAN",
                processingDurationSeconds = 10L
            )

            assertEquals("VIRUS_SCAN", event.processingType)
            assertTrue(event.outputMetadata.isEmpty())
        }

        @Test
        fun `should handle complex metadata structures`() {
            val complexMetadata = mapOf(
                "extractedText" to "Sample document content...",
                "pageCount" to 15,
                "fonts" to listOf("Arial", "Times New Roman"),
                "images" to mapOf(
                    "count" to 3,
                    "totalSize" to 524288
                ),
                "isEncrypted" to false,
                "creationDate" to "2025-08-27T10:30:00"
            )

            val event = FileProcessingCompletedEvent(
                fileId = testFileId,
                userId = testUserId,
                processingType = "METADATA_EXTRACTION",
                processingDurationSeconds = 45L,
                outputMetadata = complexMetadata
            )

            assertEquals(complexMetadata, event.outputMetadata)
            assertEquals(6, event.outputMetadata.size)
        }

        @Test
        fun `should handle zero processing duration`() {
            val event = FileProcessingCompletedEvent(
                fileId = testFileId,
                userId = testUserId,
                processingType = "CACHE_CHECK",
                processingDurationSeconds = 0L
            )

            assertEquals(0L, event.processingDurationSeconds)
        }
    }

    @Nested
    @DisplayName("FileProcessingFailedEvent Tests")
    inner class FileProcessingFailedEventTests {

        @Test
        fun `should create FileProcessingFailedEvent with required properties`() {
            val event = FileProcessingFailedEvent(
                fileId = testFileId,
                userId = testUserId,
                timestamp = testTimestamp,
                processingType = "VIRUS_SCAN",
                errorMessage = "File appears to be corrupted",
                errorCode = "SCAN_ERROR_001",
                retryCount = 2,
                canRetry = true
            )

            assertEquals(testFileId, event.fileId)
            assertEquals(testUserId, event.userId)
            assertEquals(testTimestamp, event.timestamp)
            assertEquals("FILE_PROCESSING_FAILED", event.eventType)
            assertEquals("VIRUS_SCAN", event.processingType)
            assertEquals("File appears to be corrupted", event.errorMessage)
            assertEquals("SCAN_ERROR_001", event.errorCode)
            assertEquals(2, event.retryCount)
            assertTrue(event.canRetry)
            assertNotNull(event.eventId)
        }

        @Test
        fun `should handle minimal failure information`() {
            val event = FileProcessingFailedEvent(
                fileId = testFileId,
                userId = testUserId,
                processingType = "THUMBNAIL_GENERATION",
                errorMessage = "Processing failed"
            )

            assertEquals("Processing failed", event.errorMessage)
            assertNull(event.errorCode)
            assertEquals(0, event.retryCount)
            assertFalse(event.canRetry)
        }

        @Test
        fun `should handle high retry counts`() {
            val event = FileProcessingFailedEvent(
                fileId = testFileId,
                userId = testUserId,
                processingType = "CONTENT_ANALYSIS",
                errorMessage = "Service temporarily unavailable",
                retryCount = 10,
                canRetry = false
            )

            assertEquals(10, event.retryCount)
            assertFalse(event.canRetry)
        }

        @ParameterizedTest
        @ValueSource(strings = [
            "NETWORK_TIMEOUT",
            "INVALID_FILE_FORMAT", 
            "INSUFFICIENT_MEMORY",
            "SERVICE_UNAVAILABLE",
            "PROCESSING_QUOTA_EXCEEDED"
        ])
        fun `should support different error codes`(errorCode: String) {
            val event = FileProcessingFailedEvent(
                fileId = testFileId,
                userId = testUserId,
                processingType = "VIDEO_PROCESSING",
                errorMessage = "Processing failed",
                errorCode = errorCode
            )

            assertEquals(errorCode, event.errorCode)
        }

        @Test
        fun `should handle very long error messages`() {
            val longErrorMessage = "A".repeat(1000)
            val event = FileProcessingFailedEvent(
                fileId = testFileId,
                userId = testUserId,
                processingType = "ANALYSIS",
                errorMessage = longErrorMessage
            )

            assertEquals(longErrorMessage, event.errorMessage)
            assertEquals(1000, event.errorMessage.length)
        }
    }

    @Nested
    @DisplayName("JSON Serialization Tests")
    inner class JsonSerializationTests {

        @Test
        fun `should serialize and deserialize FileUploadedEvent correctly`() {
            val originalEvent = FileUploadedEvent(
                fileId = testFileId,
                userId = testUserId,
                fileName = "test.pdf",
                originalName = "test.pdf",
                size = 1024L,
                contentType = "application/pdf",
                fileHash = "hash123",
                storagePath = "storage/path"
            )

            val json = objectMapper.writeValueAsString(originalEvent)
            val deserializedEvent = objectMapper.readValue<FileUploadedEvent>(json)

            assertEquals(originalEvent.fileId, deserializedEvent.fileId)
            assertEquals(originalEvent.userId, deserializedEvent.userId)
            assertEquals(originalEvent.fileName, deserializedEvent.fileName)
            assertEquals(originalEvent.originalName, deserializedEvent.originalName)
            assertEquals(originalEvent.size, deserializedEvent.size)
            assertEquals(originalEvent.contentType, deserializedEvent.contentType)
            assertEquals(originalEvent.fileHash, deserializedEvent.fileHash)
            assertEquals(originalEvent.storagePath, deserializedEvent.storagePath)
            assertEquals("FILE_UPLOADED", deserializedEvent.eventType)
        }

        @Test
        fun `should serialize and deserialize FileProcessingStartedEvent correctly`() {
            val originalEvent = FileProcessingStartedEvent(
                fileId = testFileId,
                userId = testUserId,
                processingType = "THUMBNAIL_GENERATION",
                estimatedDurationSeconds = 30L
            )

            val json = objectMapper.writeValueAsString(originalEvent)
            val deserializedEvent = objectMapper.readValue<FileProcessingStartedEvent>(json)

            assertEquals(originalEvent.processingType, deserializedEvent.processingType)
            assertEquals(originalEvent.estimatedDurationSeconds, deserializedEvent.estimatedDurationSeconds)
            assertEquals("FILE_PROCESSING_STARTED", deserializedEvent.eventType)
        }

        @Test
        fun `should serialize and deserialize FileProcessingCompletedEvent correctly`() {
            val metadata = mapOf("key" to "value", "number" to 42)
            val originalEvent = FileProcessingCompletedEvent(
                fileId = testFileId,
                userId = testUserId,
                processingType = "ANALYSIS",
                processingDurationSeconds = 60L,
                outputMetadata = metadata
            )

            val json = objectMapper.writeValueAsString(originalEvent)
            val deserializedEvent = objectMapper.readValue<FileProcessingCompletedEvent>(json)

            assertEquals(originalEvent.processingType, deserializedEvent.processingType)
            assertEquals(originalEvent.processingDurationSeconds, deserializedEvent.processingDurationSeconds)
            assertEquals(originalEvent.outputMetadata, deserializedEvent.outputMetadata)
            assertEquals("FILE_PROCESSING_COMPLETED", deserializedEvent.eventType)
        }

        @Test
        fun `should serialize and deserialize FileProcessingFailedEvent correctly`() {
            val originalEvent = FileProcessingFailedEvent(
                fileId = testFileId,
                userId = testUserId,
                processingType = "VIRUS_SCAN",
                errorMessage = "Scan failed",
                errorCode = "VS001",
                retryCount = 3,
                canRetry = true
            )

            val json = objectMapper.writeValueAsString(originalEvent)
            val deserializedEvent = objectMapper.readValue<FileProcessingFailedEvent>(json)

            assertEquals(originalEvent.processingType, deserializedEvent.processingType)
            assertEquals(originalEvent.errorMessage, deserializedEvent.errorMessage)
            assertEquals(originalEvent.errorCode, deserializedEvent.errorCode)
            assertEquals(originalEvent.retryCount, deserializedEvent.retryCount)
            assertEquals(originalEvent.canRetry, deserializedEvent.canRetry)
            assertEquals("FILE_PROCESSING_FAILED", deserializedEvent.eventType)
        }

        @Test
        fun `should deserialize polymorphic events using eventType`() {
            val events = listOf(
                FileUploadedEvent(
                    fileId = testFileId,
                    userId = testUserId,
                    fileName = "test.pdf",
                    originalName = "test.pdf",
                    size = 1024L,
                    contentType = "application/pdf",
                    fileHash = "hash",
                    storagePath = "path"
                ),
                FileProcessingStartedEvent(
                    fileId = testFileId,
                    userId = testUserId,
                    processingType = "ANALYSIS"
                ),
                FileProcessingCompletedEvent(
                    fileId = testFileId,
                    userId = testUserId,
                    processingType = "ANALYSIS",
                    processingDurationSeconds = 30L
                ),
                FileProcessingFailedEvent(
                    fileId = testFileId,
                    userId = testUserId,
                    processingType = "ANALYSIS",
                    errorMessage = "Failed"
                )
            )

            events.forEach { event ->
                val json = objectMapper.writeValueAsString(event)
                val deserializedEvent = objectMapper.readValue<FileEvent>(json)

                assertEquals(event.eventType, deserializedEvent.eventType)
                assertEquals(event.fileId, deserializedEvent.fileId)
                assertEquals(event.userId, deserializedEvent.userId)
                assertEquals(event::class, deserializedEvent::class)
            }
        }
    }

    @Nested
    @DisplayName("Event Inheritance Tests")
    inner class EventInheritanceTests {

        @Test
        fun `should inherit from FileEvent base class correctly`() {
            val events: List<FileEvent> = listOf(
                FileUploadedEvent(
                    fileId = testFileId,
                    userId = testUserId,
                    fileName = "test.pdf",
                    originalName = "test.pdf",
                    size = 1024L,
                    contentType = "application/pdf",
                    fileHash = "hash",
                    storagePath = "path"
                ),
                FileProcessingStartedEvent(
                    fileId = testFileId,
                    userId = testUserId,
                    processingType = "ANALYSIS"
                ),
                FileProcessingCompletedEvent(
                    fileId = testFileId,
                    userId = testUserId,
                    processingType = "ANALYSIS",
                    processingDurationSeconds = 30L
                ),
                FileProcessingFailedEvent(
                    fileId = testFileId,
                    userId = testUserId,
                    processingType = "ANALYSIS",
                    errorMessage = "Failed"
                )
            )

            events.forEach { event ->
                assertTrue(event is FileEvent)
                assertNotNull(event.eventId)
                assertEquals(testFileId, event.fileId)
                assertEquals(testUserId, event.userId)
                assertNotNull(event.timestamp)
                assertNotNull(event.eventType)
            }
        }

        @Test
        fun `should have unique event types for each event class`() {
            val eventTypes = setOf(
                FileUploadedEvent(
                    fileId = testFileId,
                    userId = testUserId,
                    fileName = "test.pdf",
                    originalName = "test.pdf",
                    size = 1024L,
                    contentType = "application/pdf",
                    fileHash = "hash",
                    storagePath = "path"
                ).eventType,
                FileProcessingStartedEvent(
                    fileId = testFileId,
                    userId = testUserId,
                    processingType = "TEST"
                ).eventType,
                FileProcessingCompletedEvent(
                    fileId = testFileId,
                    userId = testUserId,
                    processingType = "TEST",
                    processingDurationSeconds = 0L
                ).eventType,
                FileProcessingFailedEvent(
                    fileId = testFileId,
                    userId = testUserId,
                    processingType = "TEST",
                    errorMessage = "Error"
                ).eventType
            )

            assertEquals(4, eventTypes.size)
            assertTrue(eventTypes.contains("FILE_UPLOADED"))
            assertTrue(eventTypes.contains("FILE_PROCESSING_STARTED"))
            assertTrue(eventTypes.contains("FILE_PROCESSING_COMPLETED"))
            assertTrue(eventTypes.contains("FILE_PROCESSING_FAILED"))
        }
    }
}
