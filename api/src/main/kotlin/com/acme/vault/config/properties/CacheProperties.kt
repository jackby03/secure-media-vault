package com.acme.vault.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * Propiedades de configuración para Cache
 * Fase 4.1 - TTL configurables por tipo de cache
 */
@ConfigurationProperties(prefix = "app.cache")
data class CacheProperties(
    val fileMetadata: CacheConfig = CacheConfig(),
    val searchResults: CacheConfig = CacheConfig(),
    val thumbnails: CacheConfig = CacheConfig(),
    val userFiles: CacheConfig = CacheConfig()
) {
    data class CacheConfig(
        val ttl: Duration = Duration.ofMinutes(30),
        val maxSize: Long = 1000,
        val enabled: Boolean = true
    )
}

@Configuration
@EnableConfigurationProperties(CacheProperties::class)
class CacheConfiguration