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
        return Mono.justOrEmpty(
            exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        )
        .filter { authHeader -> authHeader.startsWith(BEARER_PREFIX) }
        .map { authHeader -> authHeader.substring(BEARER_PREFIX.length) }
        .map { token ->
            UsernamePasswordAuthenticationToken(null, token)
        }
    }
}
