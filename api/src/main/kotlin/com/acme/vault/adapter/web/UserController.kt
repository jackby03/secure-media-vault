package com.acme.vault.adapter.web

import com.acme.vault.adapter.web.dto.UserRequest
import com.acme.vault.adapter.web.dto.UserResponse
import com.acme.vault.adapter.web.mapper.UserMapper
import com.acme.vault.adapter.web.util.AuthenticationHelper
import com.acme.vault.application.service.UserServiceImpl
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
    private val userService: UserServiceImpl,
    private val userMapper: UserMapper,
    private val authHelper: AuthenticationHelper
) {
    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    fun create(@RequestBody userRequest: UserRequest, authentication: Authentication): Mono<UserResponse> {
        val currentUserRole = authHelper.getUserRoleFromAuthentication(authentication)
        val user = userMapper.toModel(userRequest)
        
        return userService.createUserWithRoleValidation(user, userRequest.role, currentUserRole)
            .map { createdUser: User? ->
                createdUser?.let { userMapper.toResponse(it) } 
                    ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "User not created")
            }
            .onErrorMap { error: Throwable ->
                when (error) {
                    is IllegalArgumentException -> ResponseStatusException(HttpStatus.FORBIDDEN, error.message)
                    else -> ResponseStatusException(HttpStatus.BAD_REQUEST, "User creation failed")
                }
            }
    }

    @GetMapping
    fun findAll(): Flux<UserResponse> =
        userService.findByAll()
            .map { userMapper.toResponse(it) }

    @GetMapping("/{uuid}")
    fun findByUUID(@PathVariable uuid: UUID): Mono<UserResponse> =
        userService.findByUUID(uuid)
            .map { user: User? ->
                user?.let { userMapper.toResponse(it) } 
                    ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
            }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    fun deleteByUUID(@PathVariable uuid: UUID, authentication: Authentication): Mono<Void> {
        val currentUserRole = authHelper.getUserRoleFromAuthentication(authentication)
        
        return userService.deleteUserWithRoleValidation(uuid, currentUserRole)
            .flatMap { wasDeleted: Boolean ->
                if (!wasDeleted) {
                    Mono.error<Void>(ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                } else {
                    Mono.empty<Void>()
                }
            }
            .onErrorMap { error: Throwable ->
                when (error) {
                    is IllegalArgumentException -> ResponseStatusException(HttpStatus.FORBIDDEN, error.message)
                    is ResponseStatusException -> error
                    else -> ResponseStatusException(HttpStatus.BAD_REQUEST, "Delete operation failed")
                }
            }
    }
}