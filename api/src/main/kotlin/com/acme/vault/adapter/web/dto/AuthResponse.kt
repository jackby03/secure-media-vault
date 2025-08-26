package com.acme.vault.adapter.web.dto

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val email: String,
    val role: String
)
