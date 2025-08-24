package com.acme.vault.adapter.`in`.web

import com.acme.vault.adapter.`in`.web.dto.UserRequest
import com.acme.vault.adapter.`in`.web.dto.UserResponse
import com.acme.vault.application.service.UserServiceImpl
import com.acme.vault.domain.models.Role
import com.acme.vault.domain.models.User
import org.springframework.http.HttpStatus
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
    fun create(@RequestBody userRequest: UserRequest): Mono<UserResponse> =
        userService.createUser(
            user = userRequest.toModel()
        ).map { user ->
            user?.toResponse() ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "User not created"
            )
        }

    @GetMapping
    fun findAll(): Flux<UserResponse> =
        userService.findByAll()
            .map { it.toResponse() }

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
    fun deleteByUUID(@PathVariable uuid: UUID): Mono<Void> =
        userService.deleteByUUID(uuid)
            .flatMap { isDeleted ->
                if (!isDeleted) {
                    Mono.error(ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                    ))
                } else {
                    Mono.empty()
                }
            }

    private fun UserRequest.toModel(): User =
        User(
            id = UUID.randomUUID(),
            email = this.email,
            password = this.password,
            role = Role.VIEWER
        )

    private fun User.toResponse(): UserResponse =
        UserResponse(
            uuid = this.id,
            email = this.email
        )
}