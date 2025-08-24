package com.acme.vault.adapter.web

import com.acme.vault.adapter.web.dto.UserRequest
import com.acme.vault.adapter.web.dto.UserResponse
import com.acme.vault.application.service.UserServiceImpl
import com.acme.vault.domain.models.Role
import com.acme.vault.domain.models.User
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserServiceImpl
) {
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    fun create(@RequestBody userRequest: UserRequest, authentication: Authentication): Mono<UserResponse> {
        val currentUserRole = getUserRoleFromAuthentication(authentication)
        val targetRole = determineTargetRole(userRequest.role, currentUserRole)
        
        return userService.createUser(
            user = userRequest.toModel(targetRole)
        ).map { user ->
            user?.toResponse() ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "User not created"
            )
        }
    }

    @GetMapping
    fun findAll(): Flux<UserResponse> {
        println("=== USER CONTROLLER WEB: findAll() called ===")
        return userService.findByAll()
            .doOnNext { user -> println("Found user in web controller: ${user.email}") }
            .map { it.toResponse() }
            .doOnComplete { println("Completed findAll in web controller") }
            .doOnError { error -> println("Error in findAll web controller: ${error.message}") }
    }

    @GetMapping("/{uuid}")
    fun findByUUID(@PathVariable uuid: UUID): Mono<UserResponse> =
        userService.findByUUID(uuid)
            .map { user ->
                user?.toResponse() ?: throw ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
                )
            }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    fun deleteByUUID(@PathVariable uuid: UUID, authentication: Authentication): Mono<Void> {
        val currentUserRole = getUserRoleFromAuthentication(authentication)
        
        return userService.findByUUID(uuid)
            .flatMap { userToDelete ->
                if (userToDelete == null) {
                    Mono.error(
                        ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "User not found"
                        )
                    )
                } else {
                    // Validar que EDITOR no puede eliminar ADMIN
                    if (currentUserRole == Role.EDITOR && userToDelete.role == Role.ADMIN) {
                        Mono.error(
                            ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Editors cannot delete admin users"
                            )
                        )
                    } else {
                        userService.deleteByUUID(uuid)
                            .flatMap { isDeleted ->
                                if (!isDeleted) {
                                    Mono.error(
                                        ResponseStatusException(
                                            HttpStatus.NOT_FOUND,
                                            "User not found"
                                        )
                                    )
                                } else {
                                    Mono.empty()
                                }
                            }
                    }
                }
            }
    }

    private fun UserRequest.toModel(role: Role): User =
        User(
            email = this.email,
            password = this.password,
            role = role
        )

    private fun User.toResponse(): UserResponse =
        UserResponse(
            id = this.id ?: UUID.randomUUID(), // Fallback si por algún motivo es null
            email = this.email,
            role = this.role.name,
            enabled = this.enabled,
            createdAt = this.createdAt
        )

    private fun getUserRoleFromAuthentication(authentication: Authentication): Role {
        val authorities = authentication.authorities
        return when {
            authorities.any { it.authority == "ROLE_ADMIN" } -> Role.ADMIN
            authorities.any { it.authority == "ROLE_EDITOR" } -> Role.EDITOR
            authorities.any { it.authority == "ROLE_VIEWER" } -> Role.VIEWER
            else -> Role.VIEWER
        }
    }

    private fun determineTargetRole(requestedRole: Role?, currentUserRole: Role): Role {
        return when (currentUserRole) {
            Role.ADMIN -> {
                // ADMIN puede crear usuarios con cualquier rol
                requestedRole ?: Role.VIEWER
            }
            Role.EDITOR -> {
                when (requestedRole) {
                    Role.ADMIN -> throw ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Editors cannot create admin users"
                    )
                    Role.EDITOR, Role.VIEWER -> requestedRole
                    null -> Role.VIEWER // Default para EDITOR
                }
            }
            Role.VIEWER -> {
                throw ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Viewers cannot create users"
                )
            }
        }
    }
}