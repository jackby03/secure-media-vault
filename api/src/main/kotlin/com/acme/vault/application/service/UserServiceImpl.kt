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
        return userRepository.findByEmail(user.email)
            .flatMap<User?> {
                Mono.empty()
            }
            .switchIfEmpty(
                userRepository.save(user).map { it as User? }
            )
    }

    override fun findByUUID(uuid: UUID): Mono<User?> {
        return userRepository.findById(uuid)
            .map { it as User? }
            .switchIfEmpty(Mono.empty())
    }

    override fun findByAll(): Flux<User> =
        userRepository.findAll()

    override fun deleteByUUID(uuid: UUID): Mono<Boolean> =
        userRepository.deleteByUUID(uuid)
            .thenReturn(true)
            .onErrorReturn(false)
}