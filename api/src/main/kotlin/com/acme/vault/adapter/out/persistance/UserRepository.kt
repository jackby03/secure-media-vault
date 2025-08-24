package com.acme.vault.adapter.out.persistance

import com.acme.vault.domain.models.User
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
interface UserRepository : ReactiveCrudRepository<User, UUID> {

    @Query("SELECT * FROM users WHERE email = :email")
    fun findByEmail(email: String): Mono<User>

    @Query("SELECT * FROM users WHERE enabled = true")
    fun findAllEnabled(): Flux<User>

    @Query("SELECT * FROM users WHERE role = :role")
    fun findByRole(role: String): Flux<User>

    @Query("UPDATE users SET enabled = false WHERE id = :id")
    fun softDeleteById(id: UUID): Mono<Void>

    @Query("DELETE FROM users WHERE id = :id")
    fun deleteByUUID(uuid: UUID): Mono<Void>
}