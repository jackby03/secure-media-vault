package com.acme.vault.config.security

import com.acme.vault.application.service.TokenService
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationManager(
    private val tokenService: TokenService,
    private val userDetailsService: ReactiveUserDetailsService
) : ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        val token = authentication.credentials as String

        return Mono.just(token)
            .flatMap { jwt ->
                val email = tokenService.extractEmail(jwt)
                if (email != null && !tokenService.isExpired(jwt)) {
                    userDetailsService.findByUsername(email)
                        .map { userDetails ->
                            if (tokenService.isValid(jwt, userDetails)) {
                                UsernamePasswordAuthenticationToken(
                                    userDetails.username,
                                    null,
                                    userDetails.authorities
                                )
                            } else {
                                throw org.springframework.security.authentication.BadCredentialsException("Invalid JWT token")
                            }
                        }
                } else {
                    Mono.error(org.springframework.security.authentication.BadCredentialsException("Invalid or expired JWT token"))
                }
            }
    }
}
