package com.acme.vault.adapter.web.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class FileUploadRequest(
    @field:NotBlank(message = "Filename is required")
    @field:Size(max = 255, message = "Filename must be at most 255 characters")
    val filename: String,

    @field:NotBlank(message = "Content type is required")
    @field:Size(max = 100, message = "Content type must be at most 100 characters")
    val contentType: String,

    @field:Size(max = 1000, message = "Description must be at most 1000 characters")
    val description: String? = null,

    val tags: List<String> = emptyList()
)
