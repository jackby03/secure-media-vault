package com.acme.vault.adapter.web.dto

import jakarta.validation.constraints.NotBlank

data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token es obligatorio")
    val refreshToken: String
)
