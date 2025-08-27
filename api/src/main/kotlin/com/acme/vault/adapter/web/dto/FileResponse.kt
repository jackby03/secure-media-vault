package com.acme.vault.adapter.web.dto

import com.acme.vault.domain.models.FileStatus
import java.time.LocalDateTime
import java.util.*

data class FileResponse(
    val id: UUID,
    val name: String,
    val originalName: String,
    val size: Long,
    val contentType: String,
    val fileHash: String,
    val ownerId: UUID,
    val status: FileStatus,
    val tags: List<String>,
    val description: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val processedAt: LocalDateTime?
)
