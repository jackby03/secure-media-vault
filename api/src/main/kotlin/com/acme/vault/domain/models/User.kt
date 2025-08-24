package com.acme.vault.domain.models

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("users")
data class User(
    @Id val id: UUID,
    val email: String,
    val password: String,
    val role: Role = Role.VIEWER,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val enabled: Boolean = true
)