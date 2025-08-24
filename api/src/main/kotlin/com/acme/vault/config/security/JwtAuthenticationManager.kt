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
        println("=== JWT MANAGER DEBUG ===")
        val token = authentication.credentials as String
        println("Received token for validation: ${token.take(20)}...")

        return Mono.just(token)
            .flatMap { jwt ->
                val email = tokenService.extractEmail(jwt)
                println("Extracted email from token: $email")

                val isExpired = tokenService.isExpired(jwt)
                println("Token expired: $isExpired")

                if (email != null && !isExpired) {
                    println("Token valid, looking up user: $email")
                    userDetailsService.findByUsername(email)
                        .doOnSuccess { userDetails ->
                            println("Found user: ${userDetails?.username}, authorities: ${userDetails?.authorities}")
                        }
                        .map { userDetails ->
                            val isTokenValid = tokenService.isValid(jwt, userDetails)
                            println("Token validation result: $isTokenValid")

                            if (isTokenValid) {
                                println("JWT Manager SUCCESS: Created authenticated user with roles: ${userDetails.authorities}")
                                UsernamePasswordAuthenticationToken(
                                    userDetails.username,
                                    null,
                                    userDetails.authorities
                                ) as Authentication
                            } else {
                                println("JWT Manager ERROR: Token validation failed")
                                throw org.springframework.security.authentication.BadCredentialsException("Invalid JWT token")
                            }
                        }
                } else {
                    println("JWT Manager ERROR: Invalid or expired token - email: $email, expired: $isExpired")
                    Mono.error<Authentication>(org.springframework.security.authentication.BadCredentialsException("Invalid or expired JWT token"))
                }
            }
            .doOnError { error ->
                println("JWT Manager EXCEPTION: ${error.message}")
            }
    }
}
