package com.acme.vault.config

import com.acme.vault.config.properties.JwtProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
@EnableReactiveMethodSecurity
class ApplicationConfig
