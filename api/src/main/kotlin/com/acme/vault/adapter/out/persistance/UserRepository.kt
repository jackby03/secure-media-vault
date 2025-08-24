package com.acme.vault.adapter.out.persistance

import com.acme.vault.domain.models.Role
import com.acme.vault.domain.models.User
import com.acme.vault.domain.repository.IUserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class UserRepository(
) : IUserRepository {

    private val users = mutableListOf<User>(
        User(
            id = UUID.randomUUID(),
            email = "email-sample1@mail.com",
            password = "password-sample1",
            role = Role.ADMIN
        ),
        User(
            id = UUID.randomUUID(),
            email = "email-sample2@mail.com",
            password = "password-sample2",
            role = Role.EDITOR
        ),
        User(
            id = UUID.randomUUID(),
            email = "email-sample3@mail.com",
            password = "password-sample3",
            role = Role.VIEWER
        )
    )

    override fun save(user: User): Boolean {
        return users.add(user)
    }

    override fun findByEmail(email: String): User? =
        users
            .firstOrNull { it.email == email }

    override fun findAll(): List<User> =
        users

    override fun findByUUID(uuid: UUID): User? =
        users
            .firstOrNull { it.id == uuid }

    override fun deleteByUUID(uuid: UUID): Boolean {
        val foundUser = findByUUID(uuid)
        return foundUser?.let { users.remove(it) }
            ?: false
    }

}