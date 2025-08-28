package com.acme.vault.adapter.web.util

import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class FileUtils {

    companion object {
        private val ALLOWED_CONTENT_TYPES = setOf(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml",
            "application/pdf", "text/plain", "text/csv",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/zip", "application/x-rar-compressed", "application/x-7z-compressed",
            "video/mp4", "video/avi", "video/quicktime", "video/x-msvideo",
            "audio/mpeg", "audio/wav", "audio/ogg"
        )

        private const val MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024
        private const val MAX_FILENAME_LENGTH = 255
        private const val STORAGE_PATH_PATTERN = "yyyyMMdd_HHmmss"
        private const val UUID_SUBSTRING_LENGTH = 8
    }

    fun generateStorageFileName(originalName: String, userId: UUID): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(STORAGE_PATH_PATTERN))
        val randomId = UUID.randomUUID().toString().substring(0, UUID_SUBSTRING_LENGTH)
        val extension = getFileExtension(originalName)
        
        return "users/$userId/$timestamp-$randomId$extension"
    }

    fun getFileExtension(filename: String): String {
        val lastDotIndex = filename.lastIndexOf('.')
        return if (lastDotIndex > 0 && lastDotIndex < filename.length - 1) {
            filename.substring(lastDotIndex).lowercase()
        } else {
            ""
        }
    }

    fun validateFileSize(size: Long): ValidationResult = when {
        size <= 0 -> ValidationResult.invalid("File size must be greater than 0")
        size > MAX_FILE_SIZE -> ValidationResult.invalid("File size exceeds maximum allowed size of 5GB")
        else -> ValidationResult.valid()
    }

    fun validateContentType(contentType: String): ValidationResult {
        return if (ALLOWED_CONTENT_TYPES.contains(contentType.lowercase())) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid("Content type '$contentType' is not allowed")
        }
    }

    fun validateFileName(filename: String): ValidationResult = when {
        filename.isBlank() -> ValidationResult.invalid("Filename cannot be empty")
        filename.length > MAX_FILENAME_LENGTH -> ValidationResult.invalid("Filename is too long (max $MAX_FILENAME_LENGTH characters)")
        filename.contains("..") -> ValidationResult.invalid("Filename cannot contain '..'")
        filename.matches(Regex(".*[<>:\"|?*].*")) -> ValidationResult.invalid("Filename contains invalid characters")
        else -> ValidationResult.valid()
    }

    fun sanitizeFileName(filename: String): String {
        return filename
            .replace(Regex("[<>:\"|?*]"), "_")
            .replace("..", "_")
            .trim()
            .take(MAX_FILENAME_LENGTH)
    }

    fun calculateSha256Hash(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        
        val units = arrayOf("KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = -1
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return "%.2f %s".format(size, units[unitIndex])
    }

    fun isImageFile(contentType: String): Boolean = contentType.startsWith("image/")
    
    fun isVideoFile(contentType: String): Boolean = contentType.startsWith("video/")
    
    fun isAudioFile(contentType: String): Boolean = contentType.startsWith("audio/")

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    ) {
        companion object {
            fun valid() = ValidationResult(true)
            fun invalid(message: String) = ValidationResult(false, message)
        }
    }
}
