package com.acme.vault.adapter.web

import com.acme.vault.adapter.web.dto.AuthResponse
import com.acme.vault.adapter.web.dto.LoginRequest
import com.acme.vault.adapter.web.dto.MessageResponse
import com.acme.vault.adapter.web.dto.RefreshTokenRequest
import com.acme.vault.application.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody loginRequest: LoginRequest): Mono<ResponseEntity<Any>> {
        return authService.authenticate(loginRequest.email, loginRequest.password)
            .map { authResponse ->
                ResponseEntity.ok(authResponse as Any)
            }
            .switchIfEmpty(
                Mono.just(
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(MessageResponse("Credenciales inválidas") as Any)
                )
            )
            .onErrorResume {
                Mono.just(
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(MessageResponse("Error interno del servidor") as Any)
                )
            }
    }

    @PostMapping("/refresh")
    fun refreshToken(@Valid @RequestBody refreshRequest: RefreshTokenRequest): Mono<ResponseEntity<Any>> {
        return authService.refreshToken(refreshRequest.refreshToken)
            .map { authResponse ->
                ResponseEntity.ok(authResponse as Any)
            }
            .switchIfEmpty(
                Mono.just(
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(MessageResponse("Refresh token inválido") as Any)
                )
            )
            .onErrorResume {
                Mono.just(
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(MessageResponse("Refresh token inválido") as Any)
                )
            }
    }

    @PostMapping("/logout")
    fun logout(): Mono<ResponseEntity<MessageResponse>> {
        return Mono.just(
            ResponseEntity.ok(MessageResponse("Logout exitoso"))
        )
    }
}