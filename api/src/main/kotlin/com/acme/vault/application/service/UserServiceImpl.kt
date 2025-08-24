package com.acme.vault.application.service

import com.acme.vault.adapter.out.persistance.UserRepository
import com.acme.vault.domain.models.Role
import com.acme.vault.domain.models.User
import com.acme.vault.domain.service.IUserService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : IUserService {
    override fun createUser(user: User): Mono<User?> {
        println("=== USER SERVICE: createUser() called for email: ${user.email} ===")
        return userRepository.findByEmail(user.email)
            .flatMap<User?> { existingUser ->
                println("User already exists: ${existingUser.email}")
                Mono.error(RuntimeException("User with email ${user.email} already exists"))
            }
            .switchIfEmpty(
                Mono.defer {
                    println("Creating new user with email: ${user.email}")
                    val userWithHashedPassword = user.copy(
                        password = passwordEncoder.encode(user.password)
                    )
                    println("Password hashed for user: ${user.email}")
                    
                    userRepository.save(userWithHashedPassword)
                        .map { savedUser ->
                            println("User created successfully: ${savedUser.id}")
                            savedUser as User?
                        }
                }
            )
    }

    override fun findByUUID(uuid: UUID): Mono<User?> {
        return userRepository.findById(uuid)
            .map { it as User? }
            .switchIfEmpty(Mono.empty())
    }

    override fun findByAll(): Flux<User> {
        println("=== USER SERVICE: findByAll() called ===")
        return userRepository.findAll()
            .doOnNext { user -> println("Repository returned user: ${user.email}") }
            .doOnComplete { println("Repository findAll completed") }
            .doOnError { error -> println("Repository findAll error: ${error.message}") }
    }

    override fun deleteByUUID(uuid: UUID): Mono<Boolean> =
        userRepository.deleteByUUID(uuid)
            .thenReturn(true)
            .onErrorReturn(false)

    // === BUSINESS LOGIC METHODS ===
    
    override fun createUserWithRoleValidation(user: User, requestedRole: Role?, currentUserRole: Role): Mono<User?> {
        val targetRole = determineTargetRole(requestedRole, currentUserRole)
        val userWithTargetRole = user.copy(role = targetRole)
        return createUser(userWithTargetRole)
    }
    
    override fun deleteUserWithRoleValidation(uuid: UUID, currentUserRole: Role): Mono<Boolean> {
        return findByUUID(uuid)
            .flatMap { userToDelete ->
                if (userToDelete == null) {
                    Mono.error(IllegalArgumentException("User not found"))
                } else {
                    validateDeletePermission(currentUserRole, userToDelete.role)
                        .then(deleteByUUID(uuid))
                }
            }
    }
    
    // === PRIVATE HELPER METHODS ===
    
    private fun determineTargetRole(requestedRole: Role?, currentUserRole: Role): Role {
        return when (currentUserRole) {
            Role.ADMIN -> {
                requestedRole ?: Role.VIEWER
            }
            Role.EDITOR -> {
                when (requestedRole) {
                    Role.ADMIN -> throw IllegalArgumentException("Editors cannot create admin users")
                    Role.EDITOR, Role.VIEWER -> requestedRole
                    null -> Role.VIEWER
                }
            }
            Role.VIEWER -> {
                throw IllegalArgumentException("Viewers cannot create users")
            }
        }
    }
    
    private fun validateDeletePermission(currentUserRole: Role, targetUserRole: Role): Mono<Void> {
        return if (currentUserRole == Role.EDITOR && targetUserRole == Role.ADMIN) {
            Mono.error(IllegalArgumentException("Editors cannot delete admin users"))
        } else {
            Mono.empty()
        }
    }
}