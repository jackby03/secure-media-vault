package com.acme.vault.adapter.controller.out.service

import com.acme.vault.adapter.controller.out.persistance.UserRepository
import com.acme.vault.domain.models.User
import com.acme.vault.domain.service.IUserService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserServiceImpl(
    private val userRepository: UserRepository
) : IUserService {
    override fun createUser(user: User): User? {
        val isSaved = userRepository.save(user)
        return if (isSaved) user else null
    }

    override fun findByUUID(uuid: UUID): User? =
        userRepository.findByUUID(uuid)

    override fun findByAll(): List<User> =
        userRepository.findAll()

    override fun deleteByUUID(uuid: UUID): Boolean =
        userRepository.deleteByUUID(uuid)
}