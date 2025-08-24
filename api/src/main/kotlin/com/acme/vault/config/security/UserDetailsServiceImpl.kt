package com.acme.vault.config.security

import com.acme.vault.adapter.out.persistance.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class UserDetailsServiceImpl(
    private val userRepository: UserRepository
) : ReactiveUserDetailsService {

    override fun findByUsername(username: String): Mono<UserDetails> {
        return userRepository.findByEmail(username)
            .map { user ->
                User.builder()
                    .username(user.email)
                    .password(user.password)
                    .authorities(SimpleGrantedAuthority("ROLE_${user.role.name}"))
                    .accountExpired(false)
                    .accountLocked(!user.enabled)
                    .credentialsExpired(false)
                    .disabled(!user.enabled)
                    .build()
            }
    }
}
