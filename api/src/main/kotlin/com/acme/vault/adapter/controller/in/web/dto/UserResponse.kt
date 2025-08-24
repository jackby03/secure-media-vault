package com.acme.vault.adapter.controller.`in`.web.dto

import java.util.UUID

data class UserResponse(
    val uuid: UUID,
    val email: String
)
