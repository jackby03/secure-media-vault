package com.acme.vault.domain.service

import com.acme.vault.domain.models.Role
import com.acme.vault.domain.models.User
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface IUserService {
    fun createUser(user: User): Mono<User?>
    fun createUserWithRoleValidation(user: User, requestedRole: Role?, currentUserRole: Role): Mono<User?>
    fun findByUUID(uuid: UUID): Mono<User?>
    fun findByAll(): Flux<User>
    fun deleteByUUID(uuid: UUID): Mono<Boolean>
    fun deleteUserWithRoleValidation(uuid: UUID, currentUserRole: Role): Mono<Boolean>
}