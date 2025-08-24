package com.acme.vault.config.security

import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationConverter : ServerAuthenticationConverter {

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
        println("=== JWT CONVERTER DEBUG ===")
        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        println("Authorization header: $authHeader")

        return Mono.justOrEmpty(authHeader)
            .filter { header ->
                val hasBearer = header.startsWith(BEARER_PREFIX)
                println("Has Bearer prefix: $hasBearer")
                hasBearer
            }
            .map { header ->
                val token = header.substring(BEARER_PREFIX.length)
                println("Extracted token: ${token.take(20)}...")
                token
            }
            .map { token ->
                val auth: Authentication = UsernamePasswordAuthenticationToken(null, token)
                println("Created authentication object with token")
                auth
            }
            .doOnSuccess { auth ->
                println("JWT Converter SUCCESS: ${if (auth != null) "Authentication created" else "No authentication"}")
            }
            .doOnError { error ->
                println("JWT Converter ERROR: ${error.message}")
            }
    }
}
