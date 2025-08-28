package com.acme.vault.adapter.web.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.*

@DisplayName("FileUtils Tests")
class FileUtilsTest {

    private val fileUtils = FileUtils()
    private val testUserId = UUID.randomUUID()

    @Nested
    @DisplayName("File Extension Tests")
    inner class FileExtensionTests {

        @ParameterizedTest
        @CsvSource(
            "document.pdf,.pdf",
            "image.jpeg,.jpeg",
            "archive.tar.gz,.gz",
            "file.txt,.txt",
            "image.PNG,.png",
            "document.DOCX,.docx"
        )
        fun `should extract file extension correctly`(fileName: String, expectedExtension: String) {
            val result = fileUtils.getFileExtension(fileName)
            assertEquals(expectedExtension, result)
        }

        @ParameterizedTest
        @ValueSource(strings = ["file", ".", "file.", ".hiddenfile"])
        fun `should return empty string for invalid extensions`(fileName: String) {
            val result = fileUtils.getFileExtension(fileName)
            assertEquals("", result)
        }
    }

    @Nested
    @DisplayName("Storage File Name Generation Tests")
    inner class StorageFileNameTests {

        @Test
        fun `should generate unique storage file names`() {
            val originalName = "test-document.pdf"
            
            val result1 = fileUtils.generateStorageFileName(originalName, testUserId)
            val result2 = fileUtils.generateStorageFileName(originalName, testUserId)
            
            assertNotEquals(result1, result2)
            assertTrue(result1.startsWith("users/$testUserId/"))
            assertTrue(result1.endsWith(".pdf"))
            assertTrue(result2.startsWith("users/$testUserId/"))
            assertTrue(result2.endsWith(".pdf"))
        }

        @Test
        fun `should preserve file extension in storage name`() {
            val originalName = "document.docx"
            
            val result = fileUtils.generateStorageFileName(originalName, testUserId)
            
            assertTrue(result.endsWith(".docx"))
            assertTrue(result.contains("users/$testUserId/"))
        }

        @Test
        fun `should handle files without extension`() {
            val originalName = "README"
            
            val result = fileUtils.generateStorageFileName(originalName, testUserId)
            
            assertTrue(result.startsWith("users/$testUserId/"))
            assertFalse(result.endsWith("."))
        }
    }

    @Nested
    @DisplayName("File Size Validation Tests")
    inner class FileSizeValidationTests {

        @Test
        fun `should reject zero or negative file sizes`() {
            assertEquals(false, fileUtils.validateFileSize(0).isValid)
            assertEquals(false, fileUtils.validateFileSize(-1).isValid)
            assertEquals(false, fileUtils.validateFileSize(-100).isValid)
        }

        @Test
        fun `should accept valid file sizes`() {
            assertEquals(true, fileUtils.validateFileSize(1).isValid)
            assertEquals(true, fileUtils.validateFileSize(1024).isValid)
            assertEquals(true, fileUtils.validateFileSize(1024 * 1024).isValid) // 1MB
        }

        @Test
        fun `should reject files exceeding 5GB limit`() {
            val fiveGBPlus = 5L * 1024 * 1024 * 1024 + 1
            val result = fileUtils.validateFileSize(fiveGBPlus)
            
            assertEquals(false, result.isValid)
            assertTrue(result.errorMessage!!.contains("5GB"))
        }

        @Test
        fun `should accept files at 5GB limit`() {
            val fiveGB = 5L * 1024 * 1024 * 1024
            val result = fileUtils.validateFileSize(fiveGB)
            
            assertEquals(true, result.isValid)
        }
    }

    @Nested
    @DisplayName("Content Type Validation Tests")
    inner class ContentTypeValidationTests {

        @ParameterizedTest
        @ValueSource(strings = [
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf", "text/plain", "text/csv",
            "application/zip", "video/mp4", "audio/mpeg"
        ])
        fun `should accept allowed content types`(contentType: String) {
            val result = fileUtils.validateContentType(contentType)
            assertTrue(result.isValid)
        }

        @ParameterizedTest
        @ValueSource(strings = [
            "application/exe", "text/html", "application/javascript",
            "image/bmp", "application/unknown", "video/wmv"
        ])
        fun `should reject disallowed content types`(contentType: String) {
            val result = fileUtils.validateContentType(contentType)
            assertEquals(false, result.isValid)
            assertTrue(result.errorMessage!!.contains("not allowed"))
        }

        @Test
        fun `should handle case insensitive content types`() {
            assertEquals(true, fileUtils.validateContentType("IMAGE/JPEG").isValid)
            assertEquals(true, fileUtils.validateContentType("Image/Png").isValid)
            assertEquals(true, fileUtils.validateContentType("APPLICATION/PDF").isValid)
        }
    }

    @Nested
    @DisplayName("File Name Validation Tests")
    inner class FileNameValidationTests {

        @Test
        fun `should reject empty or blank file names`() {
            assertEquals(false, fileUtils.validateFileName("").isValid)
            assertEquals(false, fileUtils.validateFileName("   ").isValid)
            assertEquals(false, fileUtils.validateFileName("\t\n").isValid)
        }

        @Test
        fun `should reject file names with path traversal`() {
            val result = fileUtils.validateFileName("../../../etc/passwd")
            assertEquals(false, result.isValid)
            assertTrue(result.errorMessage!!.contains(".."))
        }

        @ParameterizedTest
        @ValueSource(strings = ["file<name", "file>name", "file:name", "file\"name", 
                                "file|name", "file?name", "file*name"])
        fun `should reject file names with invalid characters`(fileName: String) {
            val result = fileUtils.validateFileName(fileName)
            assertEquals(false, result.isValid)
            assertTrue(result.errorMessage!!.contains("invalid characters"))
        }

        @Test
        fun `should reject overly long file names`() {
            val longFileName = "a".repeat(256)
            val result = fileUtils.validateFileName(longFileName)
            
            assertEquals(false, result.isValid)
            assertTrue(result.errorMessage!!.contains("too long"))
        }

        @ParameterizedTest
        @ValueSource(strings = ["document.pdf", "image_01.jpeg", "file-name.txt", 
                                "valid file name.docx", "file123.zip"])
        fun `should accept valid file names`(fileName: String) {
            val result = fileUtils.validateFileName(fileName)
            assertTrue(result.isValid)
        }
    }

    @Nested
    @DisplayName("File Name Sanitization Tests")
    inner class FileNameSanitizationTests {

        @Test
        fun `should replace invalid characters with underscores`() {
            val result = fileUtils.sanitizeFileName("file<>:\"|?*name.txt")
            assertEquals("file_______name.txt", result)
        }

        @Test
        fun `should replace path traversal sequences`() {
            val result = fileUtils.sanitizeFileName("../../../file.txt")
            assertEquals("_/_/_/file.txt", result)
        }

        @Test
        fun `should trim whitespace and limit length`() {
            val longName = "   " + "a".repeat(300) + "   "
            val result = fileUtils.sanitizeFileName(longName)
            
            assertEquals(255, result.length)
            assertFalse(result.startsWith(" "))
            assertFalse(result.endsWith(" "))
        }

        @Test
        fun `should preserve valid file names`() {
            val validName = "valid-file_name.pdf"
            val result = fileUtils.sanitizeFileName(validName)
            assertEquals(validName, result)
        }
    }

    @Nested
    @DisplayName("SHA-256 Hash Calculation Tests")
    inner class HashCalculationTests {

        @Test
        fun `should calculate consistent SHA-256 hash`() {
            val data = "Hello, World!".toByteArray()
            
            val hash1 = fileUtils.calculateSha256Hash(data)
            val hash2 = fileUtils.calculateSha256Hash(data)
            
            assertEquals(hash1, hash2)
            assertEquals(64, hash1.length) // SHA-256 produces 64 hex characters
        }

        @Test
        fun `should produce different hashes for different data`() {
            val data1 = "Hello, World!".toByteArray()
            val data2 = "Hello, Universe!".toByteArray()
            
            val hash1 = fileUtils.calculateSha256Hash(data1)
            val hash2 = fileUtils.calculateSha256Hash(data2)
            
            assertNotEquals(hash1, hash2)
        }

        @Test
        fun `should handle empty data`() {
            val emptyData = ByteArray(0)
            val hash = fileUtils.calculateSha256Hash(emptyData)
            
            assertNotNull(hash)
            assertEquals(64, hash.length)
        }

        @Test
        fun `should produce lowercase hex string`() {
            val data = "test".toByteArray()
            val hash = fileUtils.calculateSha256Hash(data)
            
            assertEquals(hash, hash.lowercase())
            assertTrue(hash.matches(Regex("^[0-9a-f]{64}$")))
        }
    }

    @Nested
    @DisplayName("File Size Formatting Tests")
    inner class FileSizeFormattingTests {

        @ParameterizedTest
        @CsvSource(
            "0,0 B",
            "1,1 B",
            "1023,1023 B",
            "1024,1.00 KB",
            "1536,1.50 KB",
            "1048576,1.00 MB",
            "1073741824,1.00 GB",
            "1099511627776,1.00 TB"
        )
        fun `should format file sizes correctly`(bytes: Long, expected: String) {
            val result = fileUtils.formatFileSize(bytes)
            assertEquals(expected, result)
        }

        @Test
        fun `should handle large file sizes`() {
            val largeSize = 5L * 1024 * 1024 * 1024 // 5GB
            val result = fileUtils.formatFileSize(largeSize)
            assertEquals("5.00 GB", result)
        }
    }

    @Nested
    @DisplayName("File Type Detection Tests")
    inner class FileTypeDetectionTests {

        @ParameterizedTest
        @ValueSource(strings = ["image/jpeg", "image/png", "image/gif", "image/webp"])
        fun `should detect image files correctly`(contentType: String) {
            assertTrue(fileUtils.isImageFile(contentType))
            assertFalse(fileUtils.isVideoFile(contentType))
            assertFalse(fileUtils.isAudioFile(contentType))
        }

        @ParameterizedTest
        @ValueSource(strings = ["video/mp4", "video/avi", "video/quicktime"])
        fun `should detect video files correctly`(contentType: String) {
            assertTrue(fileUtils.isVideoFile(contentType))
            assertFalse(fileUtils.isImageFile(contentType))
            assertFalse(fileUtils.isAudioFile(contentType))
        }

        @ParameterizedTest
        @ValueSource(strings = ["audio/mpeg", "audio/wav", "audio/ogg"])
        fun `should detect audio files correctly`(contentType: String) {
            assertTrue(fileUtils.isAudioFile(contentType))
            assertFalse(fileUtils.isImageFile(contentType))
            assertFalse(fileUtils.isVideoFile(contentType))
        }

        @Test
        fun `should handle non-media content types`() {
            val contentType = "application/pdf"
            assertFalse(fileUtils.isImageFile(contentType))
            assertFalse(fileUtils.isVideoFile(contentType))
            assertFalse(fileUtils.isAudioFile(contentType))
        }
    }

    @Nested
    @DisplayName("ValidationResult Tests")
    inner class ValidationResultTests {

        @Test
        fun `should create valid result correctly`() {
            val result = FileUtils.ValidationResult.valid()
            assertTrue(result.isValid)
            assertNull(result.errorMessage)
        }

        @Test
        fun `should create invalid result with message`() {
            val message = "Test error message"
            val result = FileUtils.ValidationResult.invalid(message)
            
            assertFalse(result.isValid)
            assertEquals(message, result.errorMessage)
        }
    }
}
