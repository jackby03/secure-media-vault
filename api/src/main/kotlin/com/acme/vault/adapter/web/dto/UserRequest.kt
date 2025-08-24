package com.acme.vault.adapter.web.dto

import com.acme.vault.domain.models.Role

data class UserRequest(
    val email: String,
    val password: String,
    val role: Role? = null
)
