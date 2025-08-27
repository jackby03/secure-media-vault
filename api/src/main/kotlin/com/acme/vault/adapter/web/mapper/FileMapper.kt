package com.acme.vault.adapter.web.mapper

import com.acme.vault.adapter.web.dto.FileListResponse
import com.acme.vault.adapter.web.dto.FileResponse
import com.acme.vault.adapter.web.dto.FileUploadRequest
import com.acme.vault.domain.models.File
import com.acme.vault.domain.models.FileStatus
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class FileMapper {
    fun toFileResponse(file: File): FileResponse {
        return FileResponse(
            id = file.id!!,
            name = file.name,
            originalName = file.originalName,
            size = file.size,
            contentType = file.contentType,
            fileHash = file.fileHash,
            ownerId = file.ownerId,
            status = file.status,
            tags = file.tags,
            description = file.description,
            createdAt = file.createdAt!!,
            updatedAt = file.updatedAt!!,
            processedAt = file.processedAt
        )
    }

    fun toEntity(
        request: FileUploadRequest,
        name: String,
        size: Long,
        fileHash: String,
        storagePath: String,
        ownerId: UUID
    ): File {
        return File(
            id = null,
            name = name,
            originalName = request.filename,
            size = size,
            contentType = request.contentType,
            fileHash = fileHash,
            storagePath = storagePath,
            ownerId = ownerId,
            status = FileStatus.PENDING,
            tags = request.tags,
            description = request.description,
        )
    }
    fun toFileListResponse(
        files: List<File>,
        totalElements: Long,
        currentPage: Int,
        pageSize: Int
    ): FileListResponse {
        val fileResponses = files.map { toFileResponse(it) }
        val totalPages = (totalElements + pageSize - 1) / pageSize

        return FileListResponse(
            files = fileResponses,
            totalElements = totalElements,
            totalPages = totalPages.toInt(),
            currentPage = currentPage,
            pageSize = pageSize,
            hasNext = currentPage < totalPages - 1,
            hasPrevious = currentPage > 0
        )
    }
}
