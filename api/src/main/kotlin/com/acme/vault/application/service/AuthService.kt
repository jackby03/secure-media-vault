package com.acme.vault.application.service

import com.acme.vault.adapter.persistance.UserRepository
import com.acme.vault.adapter.web.dto.AuthResponse
import com.acme.vault.config.properties.JwtProperties
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.*

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenService: TokenService,
    private val jwtProperties: JwtProperties
) {

    fun authenticate(email: String, password: String): Mono<AuthResponse?> {
        return userRepository.findByEmail(email)
            .filter { user ->
                user.enabled && passwordEncoder.matches(password, user.password)
            }
            .flatMap { user ->
                val userDetails = User(
                    user.email,
                    user.password,
                    listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
                )

                Mono.fromCallable {
                    val accessTokenExpiration = Date(System.currentTimeMillis() + jwtProperties.accessTokenExpiration)
                    val refreshTokenExpiration = Date(System.currentTimeMillis() + jwtProperties.refreshTokenExpiration)

                    val accessToken = tokenService.generate(
                        userDetails = userDetails,
                        expirationDate = accessTokenExpiration,
                        additionalClaims = mapOf(
                            "role" to user.role.name,
                            "userId" to user.id.toString()
                        )
                    )

                    val refreshToken = tokenService.generate(
                        userDetails = userDetails,
                        expirationDate = refreshTokenExpiration,
                        additionalClaims = mapOf("type" to "refresh")
                    )

                    AuthResponse(
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        expiresIn = jwtProperties.accessTokenExpiration / 1000,
                        email = user.email,
                        role = user.role.name
                    )
                }
            }
            .map { it as AuthResponse? }
            .switchIfEmpty(Mono.empty())
    }

    fun refreshToken(refreshToken: String): Mono<AuthResponse?> {
        return Mono.fromCallable {
            val email = tokenService.extractEmail(refreshToken)
            if (email != null && !tokenService.isExpired(refreshToken)) {
                email
            } else {
                null
            }
        }
        .flatMap { email ->
            if (email != null) {
                userRepository.findByEmail(email)
            } else {
                Mono.empty()
            }
        }
        .flatMap { user ->
            val userDetails = User(
                user.email,
                user.password,
                listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
            )

            Mono.fromCallable {
                val accessTokenExpiration = Date(System.currentTimeMillis() + jwtProperties.accessTokenExpiration)

                val newAccessToken = tokenService.generate(
                    userDetails = userDetails,
                    expirationDate = accessTokenExpiration,
                    additionalClaims = mapOf(
                        "role" to user.role.name,
                        "userId" to user.id.toString()
                    )
                )

                AuthResponse(
                    accessToken = newAccessToken,
                    refreshToken = refreshToken,
                    expiresIn = jwtProperties.accessTokenExpiration / 1000,
                    email = user.email,
                    role = user.role.name
                )
            }
        }
        .map { it as AuthResponse? }
        .switchIfEmpty(Mono.empty())
    }
}
