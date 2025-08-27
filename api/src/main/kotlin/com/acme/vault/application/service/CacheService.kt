package com.acme.vault.application.service

import com.acme.vault.config.properties.CacheProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * Servicio de cache reactivo con Redis
 * Fase 4.1 - Cache con Redis
 */
@Service
class CacheService(
    private val redisTemplate: ReactiveRedisTemplate<String, Any>,
    private val cacheProperties: CacheProperties,
    private val objectMapper: ObjectMapper
) {

    /**
     * Cache de metadatos de archivos más accedidos
     */
    fun cacheFileMetadata(fileId: String, metadata: Any): Mono<Boolean> {
        if (!cacheProperties.fileMetadata.enabled) {
            return Mono.just(false)
        }

        val key = "file:metadata:$fileId"
        return redisTemplate.opsForValue()
            .set(key, metadata, cacheProperties.fileMetadata.ttl)
    }

    fun getFileMetadata(fileId: String): Mono<Any?> {
        if (!cacheProperties.fileMetadata.enabled) {
            return Mono.empty()
        }

        val key = "file:metadata:$fileId"
        return redisTemplate.opsForValue().get(key)
    }

    /**
     * Cache de resultados de búsquedas
     */
    fun cacheSearchResults(searchKey: String, results: Any): Mono<Boolean> {
        if (!cacheProperties.searchResults.enabled) {
            return Mono.just(false)
        }

        val key = "search:$searchKey"
        return redisTemplate.opsForValue()
            .set(key, results, cacheProperties.searchResults.ttl)
    }

    fun getSearchResults(searchKey: String): Mono<Any?> {
        if (!cacheProperties.searchResults.enabled) {
            return Mono.empty()
        }

        val key = "search:$searchKey"
        return redisTemplate.opsForValue().get(key)
    }

    /**
     * Cache de archivos por usuario
     */
    fun cacheUserFiles(userId: String, page: Int, size: Int, files: Any): Mono<Boolean> {
        if (!cacheProperties.userFiles.enabled) {
            return Mono.just(false)
        }

        val key = "user:files:$userId:$page:$size"
        return redisTemplate.opsForValue()
            .set(key, files, cacheProperties.userFiles.ttl)
    }

    fun getUserFiles(userId: String, page: Int, size: Int): Mono<Any?> {
        if (!cacheProperties.userFiles.enabled) {
            return Mono.empty()
        }

        val key = "user:files:$userId:$page:$size"
        return redisTemplate.opsForValue().get(key)
    }

    /**
     * Invalidación de cache
     */
    fun invalidateFileMetadata(fileId: String): Mono<Long> {
        val key = "file:metadata:$fileId"
        return redisTemplate.delete(key)
    }

    fun invalidateUserFilesCache(userId: String): Mono<Long> {
        val pattern = "user:files:$userId:*"
        return redisTemplate.keys(pattern)
            .collectList()
            .flatMap { keys ->
                if (keys.isNotEmpty()) {
                    redisTemplate.delete(*keys.toTypedArray())
                } else {
                    Mono.just(0L)
                }
            }
    }

    fun invalidateSearchCache(): Mono<Long> {
        val pattern = "search:*"
        return redisTemplate.keys(pattern)
            .collectList()
            .flatMap { keys ->
                if (keys.isNotEmpty()) {
                    redisTemplate.delete(*keys.toTypedArray())
                } else {
                    Mono.just(0L)
                }
            }
    }

    /**
     * Utilidades generales
     */
    fun exists(key: String): Mono<Boolean> {
        return redisTemplate.hasKey(key)
    }

    fun delete(key: String): Mono<Boolean> {
        return redisTemplate.delete(key).map { it > 0 }
    }

    fun setWithTtl(key: String, value: Any, ttl: Duration): Mono<Boolean> {
        return redisTemplate.opsForValue().set(key, value, ttl)
    }

    fun get(key: String): Mono<Any?> {
        return redisTemplate.opsForValue().get(key)
    }
}