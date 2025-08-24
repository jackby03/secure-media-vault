package com.acme.vault.config.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val jwtAuthenticationManager: ReactiveAuthenticationManager,
) {

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authenticationManager(jwtAuthenticationManager)
            .authorizeExchange { exchanges ->
                exchanges
                    // Endpoints públicos
                    .pathMatchers("/actuator/health").permitAll()
                    .pathMatchers(HttpMethod.POST, "/auth/login").permitAll()
                    .pathMatchers(HttpMethod.POST, "/auth/refresh").permitAll()

                    // Endpoints por roles
                    .pathMatchers(HttpMethod.GET, "/users").hasAnyRole("ADMIN", "EDITOR")
                    .pathMatchers(HttpMethod.POST, "/users").hasRole("ADMIN")
                    .pathMatchers(HttpMethod.DELETE, "/users/**").hasRole("ADMIN")

                    // Cualquier otro endpoint requiere autenticación
                    .anyExchange().authenticated()
            }
            .build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = listOf("*")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
