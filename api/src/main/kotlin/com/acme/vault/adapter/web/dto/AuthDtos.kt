package com.acme.vault.adapter.web.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:Email(message = "Email debe tener formato válido")
    @field:NotBlank(message = "Email es obligatorio")
    val email: String,

    @field:NotBlank(message = "Password es obligatorio")
    @field:Size(min = 6, message = "Password debe tener mínimo 6 caracteres")
    val password: String
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val email: String,
    val role: String
)

data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token es obligatorio")
    val refreshToken: String
)

data class MessageResponse(
    val message: String
)
