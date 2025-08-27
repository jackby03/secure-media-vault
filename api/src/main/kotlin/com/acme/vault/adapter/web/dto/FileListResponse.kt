package com.acme.vault.adapter.web.dto

data class FileListResponse(
    val files: List<FileResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)