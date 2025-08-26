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
