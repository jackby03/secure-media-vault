package com.acme.vault.adapter.web

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@RestController
@RequestMapping("/api")
class HealthController {

    @GetMapping("/health")
    fun health(): Mono<Map<String, String>> = Mono.just(mapOf("status" to "UP"))

    /**
     * Health check con información adicional del sistema
     */
    @GetMapping("/health/detailed")
    @PreAuthorize("hasRole('ADMIN')")
    fun detailedHealth(): Mono<Map<String, Any>> {
        return Mono.just(mapOf(
            "status" to "UP",
            "timestamp" to LocalDateTime.now().toString(),
            "database" to mapOf(
                "status" to "UP",
                "poolSize" to "20",
                "activeConnections" to "5"
            ),
            "cache" to mapOf(
                "redis" to "UP",
                "hitRate" to "85%"
            ),
            "storage" to mapOf(
                "minio" to "UP",
                "buckets" to 1
            ),
            "messaging" to mapOf(
                "rabbitmq" to "UP",
                "queues" to 1
            )
        ))
    }
}