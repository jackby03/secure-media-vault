package com.acme.vault.adapter.web.util

import com.acme.vault.domain.models.Role
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
class AuthenticationHelper {
    
    fun getUserRoleFromAuthentication(authentication: Authentication): Role {
        val authorities = authentication.authorities
        return when {
            authorities.any { it.authority == "ROLE_ADMIN" } -> Role.ADMIN
            authorities.any { it.authority == "ROLE_EDITOR" } -> Role.EDITOR
            authorities.any { it.authority == "ROLE_VIEWER" } -> Role.VIEWER
            else -> Role.VIEWER // Default fallback
        }
    }
}
