package com.acme.vault.adapter.web.dto

import java.time.LocalDateTime
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val email: String,
    val role: String,
    val enabled: Boolean,
    val createdAt: LocalDateTime
)
