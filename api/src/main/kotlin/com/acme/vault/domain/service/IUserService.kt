package com.acme.vault.domain.service

import com.acme.vault.domain.models.User
import java.util.UUID

interface IUserService {
    fun createUser(user: User): User?
    fun findByUUID(uuid: UUID): User?
    fun findByAll(): List<User>
    fun deleteByUUID(uuid: UUID): Boolean
}