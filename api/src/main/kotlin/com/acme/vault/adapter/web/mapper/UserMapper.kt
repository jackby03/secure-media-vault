package com.acme.vault.adapter.web.mapper

import com.acme.vault.adapter.web.dto.UserRequest
import com.acme.vault.adapter.web.dto.UserResponse
import com.acme.vault.domain.models.Role
import com.acme.vault.domain.models.User
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UserMapper {
    
    fun toModel(userRequest: UserRequest): User =
        User(
            email = userRequest.email,
            password = userRequest.password,
            role = Role.VIEWER
        )

    fun toResponse(user: User): UserResponse =
        UserResponse(
            id = user.id ?: UUID.randomUUID(),
            email = user.email,
            role = user.role.name,
            enabled = user.enabled,
            createdAt = user.createdAt
        )
}
