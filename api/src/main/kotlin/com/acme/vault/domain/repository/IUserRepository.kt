package com.acme.vault.domain.repository

import com.acme.vault.domain.models.User
import java.util.UUID

interface IUserRepository {
    fun save(user: User): Boolean
    fun findByEmail(email: String): User?
    fun findAll(): List<User>
    fun findByUUID(uuid: UUID): User?
    fun deleteByUUID(uuid: UUID): Boolean
}