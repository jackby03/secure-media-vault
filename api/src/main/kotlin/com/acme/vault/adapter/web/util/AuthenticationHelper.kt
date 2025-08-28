package com.acme.vault.adapter.web.util

import com.acme.vault.application.service.TokenService
import com.acme.vault.domain.models.Role
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import java.util.UUID

@Component
class AuthenticationHelper(
    private val tokenService: TokenService
) {
    
    companion object {
        private const val BEARER_PREFIX = "Bearer "
        private const val BEARER_PREFIX_LENGTH = 7
    }
    
    fun getUserRoleFromAuthentication(authentication: Authentication): Role {
        val authorities = authentication.authorities
        return when {
            authorities.any { it.authority == "ROLE_ADMIN" } -> Role.ADMIN
            authorities.any { it.authority == "ROLE_EDITOR" } -> Role.EDITOR
            authorities.any { it.authority == "ROLE_VIEWER" } -> Role.VIEWER
            else -> Role.VIEWER
        }
    }
    
    fun getUserIdFromAuthentication(authentication: Authentication, exchange: ServerWebExchange): UUID {
        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
            ?: throw IllegalStateException("Authorization header not found")
        
        if (!authHeader.startsWith(BEARER_PREFIX)) {
            throw IllegalStateException("Invalid authorization header format")
        }
        
        val token = authHeader.substring(BEARER_PREFIX_LENGTH)
        val userIdStr = tokenService.extractUserId(token)
            ?: throw IllegalStateException("Could not extract user ID from token")
        
        return runCatching { UUID.fromString(userIdStr) }
            .getOrElse { throw IllegalStateException("Invalid user ID format: $userIdStr") }
    }
}
