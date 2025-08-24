package com.acme.vault.application.service

import com.acme.vault.adapter.out.persistance.UserRepository
import com.acme.vault.domain.models.User
import com.acme.vault.domain.service.IUserService
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class UserServiceImpl(
    private val userRepository: UserRepository
) : IUserService {
    override fun createUser(user: User): Mono<User?> {
        val isSaved = userRepository.save(user)
        return if (isSaved) Mono.just(user) else Mono.empty()
    }

    override fun findByUUID(uuid: UUID): Mono<User?> =
        Mono.justOrEmpty(userRepository.findByUUID(uuid))

    override fun findByAll(): Flux<User> =
        Flux.fromIterable(userRepository.findAll())

    override fun deleteByUUID(uuid: UUID): Mono<Boolean> =
        Mono.just(userRepository.deleteByUUID(uuid))
}