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
            // Images
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml",
            // Documents
            "application/pdf", "text/plain", "text/csv",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            // Archives
            "application/zip", "application/x-rar-compressed", "application/x-7z-compressed",
            // Video
            "video/mp4", "video/avi", "video/quicktime", "video/x-msvideo",
            // Audio
            "audio/mpeg", "audio/wav", "audio/ogg"
        )

        private const val MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024 // 5GB in bytes
        private const val MAX_FILENAME_LENGTH = 255
    }

    /**
     * Genera un nombre único para el archivo en storage
     */
    fun generateStorageFileName(originalName: String, userId: UUID): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val randomId = UUID.randomUUID().toString().substring(0, 8)
        val extension = getFileExtension(originalName)
        
        return "users/$userId/$timestamp-$randomId$extension"
    }

    /**
     * Obtiene la extensión del archivo
     */
    fun getFileExtension(filename: String): String {
        val lastDotIndex = filename.lastIndexOf('.')
        return if (lastDotIndex > 0 && lastDotIndex < filename.length - 1) {
            filename.substring(lastDotIndex).lowercase()
        } else {
            ""
        }
    }

    /**
     * Valida el tamaño del archivo
     */
    fun validateFileSize(size: Long): ValidationResult {
        return when {
            size <= 0 -> ValidationResult.invalid("File size must be greater than 0")
            size > MAX_FILE_SIZE -> ValidationResult.invalid("File size exceeds maximum allowed size of 5GB")
            else -> ValidationResult.valid()
        }
    }

    /**
     * Valida el tipo de contenido del archivo
     */
    fun validateContentType(contentType: String): ValidationResult {
        return if (ALLOWED_CONTENT_TYPES.contains(contentType.lowercase())) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid("Content type '$contentType' is not allowed")
        }
    }

    /**
     * Valida el nombre del archivo
     */
    fun validateFileName(filename: String): ValidationResult {
        return when {
            filename.isBlank() -> ValidationResult.invalid("Filename cannot be empty")
            filename.length > MAX_FILENAME_LENGTH -> ValidationResult.invalid("Filename is too long (max $MAX_FILENAME_LENGTH characters)")
            filename.contains("..") -> ValidationResult.invalid("Filename cannot contain '..'")
            filename.matches(Regex(".*[<>:\"|?*].*")) -> ValidationResult.invalid("Filename contains invalid characters")
            else -> ValidationResult.valid()
        }
    }

    /**
     * Sanitiza el nombre del archivo
     */
    fun sanitizeFileName(filename: String): String {
        return filename
            .replace(Regex("[<>:\"|?*]"), "_")
            .replace("..", "_")
            .trim()
            .take(MAX_FILENAME_LENGTH)
    }

    /**
     * Calcula el hash SHA-256 de un array de bytes
     */
    fun calculateSha256Hash(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Convierte bytes a formato legible
     */
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

    /**
     * Determina si un archivo es una imagen
     */
    fun isImageFile(contentType: String): Boolean {
        return contentType.startsWith("image/")
    }

    /**
     * Determina si un archivo es un video
     */
    fun isVideoFile(contentType: String): Boolean {
        return contentType.startsWith("video/")
    }

    /**
     * Determina si un archivo es un audio
     */
    fun isAudioFile(contentType: String): Boolean {
        return contentType.startsWith("audio/")
    }

    /**
     * Resultado de validación
     */
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
