package com.acme.vault.adapter.web.util

import com.acme.vault.application.service.TokenService
import com.acme.vault.domain.models.Role
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.UUID

@Component
class AuthenticationHelper(
    private val tokenService: TokenService
) {
    
    fun getUserRoleFromAuthentication(authentication: Authentication): Role {
        val authorities = authentication.authorities
        return when {
            authorities.any { it.authority == "ROLE_ADMIN" } -> Role.ADMIN
            authorities.any { it.authority == "ROLE_EDITOR" } -> Role.EDITOR
            authorities.any { it.authority == "ROLE_VIEWER" } -> Role.VIEWER
            else -> Role.VIEWER // Default fallback
        }
    }
    
    fun getUserIdFromAuthentication(authentication: Authentication, exchange: ServerWebExchange): UUID {
        // Extraer token del header Authorization
        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7) // Remover "Bearer "
            val userIdStr = tokenService.extractUserId(token)
            if (userIdStr != null) {
                return UUID.fromString(userIdStr)
            }
        }
        // Fallback en caso de que no se pueda extraer
        throw IllegalStateException("Could not extract user ID from authentication")
    }
}
