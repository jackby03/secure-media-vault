package com.acme.vault.adapter.web

import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/processing")
class ProcessingHealthController(
    private val connectionFactory: ConnectionFactory
) {

    @GetMapping("/rabbitmq-health")
    fun rabbitMqHealth(): Mono<ResponseEntity<Map<String, Any>>> {
        return Mono.fromCallable {
            try {
                val connection = connectionFactory.createConnection()
                val isOpen = connection.isOpen
                connection.close()

                val healthInfo = mapOf(
                    "status" to if (isOpen) "UP" else "DOWN",
                    "details" to mapOf(
                        "connection" to if (isOpen) "Connected" else "Disconnected",
                        "timestamp" to java.time.LocalDateTime.now().toString()
                    )
                )

                if (isOpen) {
                    ResponseEntity.ok(healthInfo)
                } else {
                    ResponseEntity.status(503).body(healthInfo)
                }
            } catch (e: Exception) {
                val errorInfo = mapOf(
                    "status" to "DOWN",
                    "details" to mapOf(
                        "error" to (e.message ?: "Unknown error"),
                        "timestamp" to java.time.LocalDateTime.now().toString()
                    )
                )
                ResponseEntity.status(503).body(errorInfo)
            }
        }
    }

    @GetMapping("/health")
    fun processingHealth(): Mono<ResponseEntity<Map<String, Any>>> {
        return Mono.just(
            ResponseEntity.ok(
                mapOf(
                    "status" to "UP",
                    "details" to mapOf(
                        "workers" to "FileProcessingWorker active",
                        "queues" to "file.processing queue configured",
                        "timestamp" to java.time.LocalDateTime.now().toString()
                    )
                )
            )
        )
    }
}
