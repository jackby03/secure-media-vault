package com.acme.vault.domain.models

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("file_metadata")
data class File(
    @Id val id: UUID? = null,
    val name: String,
    val originalName: String,
    val size: Long,
    val contentType: String,
    val fileHash: String,
    val storagePath: String,
    val ownerId: UUID,
    val status: FileStatus = FileStatus.PENDING,
    val tags: List<String> = emptyList(),
    val description: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val processedAt: LocalDateTime? = null
)
