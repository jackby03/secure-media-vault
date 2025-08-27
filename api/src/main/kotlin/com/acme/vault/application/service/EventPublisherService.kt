package com.acme.vault.application.service

import com.acme.vault.config.properties.RabbitProperties
import com.acme.vault.domain.events.*
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/**
 * Servicio para publicar eventos de archivos a RabbitMQ
 */
@Service
class EventPublisherService(
    private val rabbitTemplate: RabbitTemplate,
    private val rabbitProperties: RabbitProperties
) {

    /**
     * Publica un evento de archivo subido
     */
    fun publishFileUploadedEvent(event: FileUploadedEvent): Mono<Void> {
        return publishEvent(event, rabbitProperties.routing.fileUploaded)
    }

    /**
     * Publica un evento de inicio de procesamiento
     */
    fun publishFileProcessingStartedEvent(event: FileProcessingStartedEvent): Mono<Void> {
        return publishEvent(event, rabbitProperties.routing.fileProcessingStarted)
    }

    /**
     * Publica un evento de procesamiento completado
     */
    fun publishFileProcessingCompletedEvent(event: FileProcessingCompletedEvent): Mono<Void> {
        return publishEvent(event, rabbitProperties.routing.fileProcessingCompleted)
    }

    /**
     * Publica un evento de procesamiento fallido
     */
    fun publishFileProcessingFailedEvent(event: FileProcessingFailedEvent): Mono<Void> {
        return publishEvent(event, rabbitProperties.routing.fileProcessingFailed)
    }

    /**
     * Método genérico para publicar cualquier evento de archivo
     */
    private fun publishEvent(event: FileEvent, routingKey: String): Mono<Void> {
        return Mono.fromRunnable<Void> {
            try {
                println("=== EVENT PUBLISHER: Publishing event: ${event.eventType} for file: ${event.fileId} ===")
                
                rabbitTemplate.convertAndSend(
                    rabbitProperties.exchanges.fileEvents,
                    routingKey,
                    event
                ) { message ->
                    message.messageProperties.headers["eventId"] = event.eventId.toString()
                    message.messageProperties.headers["fileId"] = event.fileId.toString()
                    message.messageProperties.headers["userId"] = event.userId.toString()
                    message.messageProperties.headers["eventType"] = event.eventType
                    message.messageProperties.headers["timestamp"] = event.timestamp.toString()
                    message
                }
                
                println("Event published successfully: ${event.eventType} for file: ${event.fileId}")
            } catch (e: Exception) {
                println("Error publishing event: ${event.eventType} for file: ${event.fileId}. Error: ${e.message}")
                throw e
            }
        }
            .subscribeOn(Schedulers.boundedElastic())
            .then()
            .doOnError { error ->
                println("Failed to publish event: ${event.eventType} for file: ${event.fileId}. Error: ${error.message}")
            }
    }

    /**
     * Método para publicar múltiples eventos en lote
     */
    fun publishEvents(events: List<Pair<FileEvent, String>>): Mono<Void> {
        return Mono.fromRunnable<Void> {
            events.forEach { (event, routingKey) ->
                publishEvent(event, routingKey).subscribe()
            }
        }
            .subscribeOn(Schedulers.boundedElastic())
            .then()
    }
}
