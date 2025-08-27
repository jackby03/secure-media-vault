package com.acme.vault.config.messaging

import com.acme.vault.config.properties.RabbitProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.annotation.EnableRabbit
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableRabbit
class RabbitConfig(
    private val rabbitProperties: RabbitProperties,
    private val objectMapper: ObjectMapper
) {

    // =========================
    // CONNECTION & TEMPLATE
    // =========================

    @Bean
    fun rabbitConnectionFactory(): ConnectionFactory {
        val factory = CachingConnectionFactory()
        
        try {
            factory.setHost(rabbitProperties.host)
            factory.setPort(rabbitProperties.port)
            factory.setUsername(rabbitProperties.username)
            factory.setPassword(rabbitProperties.password)
            factory.setVirtualHost(rabbitProperties.virtualHost)
            
            // Configuración de timeouts
            factory.setConnectionTimeout(10000) // 10 segundos
            factory.setRequestedHeartBeat(30) // 30 segundos
            
            println("=== RABBITMQ CONFIG: Configured connection to ${rabbitProperties.host}:${rabbitProperties.port} ===")
        } catch (e: Exception) {
            println("=== RABBITMQ CONFIG: Error configuring RabbitMQ connection: ${e.message} ===")
            println("Make sure RabbitMQ is running on ${rabbitProperties.host}:${rabbitProperties.port}")
        }
        
        return factory
    }

    @Bean
    fun messageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter(objectMapper)
    }

    @Bean
    fun rabbitTemplate(
        connectionFactory: ConnectionFactory,
        messageConverter: MessageConverter
    ): RabbitTemplate {
        val template = RabbitTemplate(connectionFactory)
        template.messageConverter = messageConverter
        template.setMandatory(true)
        template.setConfirmCallback { correlationData, ack, cause ->
            if (ack) {
                println("Message confirmed: ${correlationData?.id}")
            } else {
                println("Message not confirmed: ${correlationData?.id}, cause: $cause")
            }
        }
        return template
    }

    // =========================
    // EXCHANGES
    // =========================

    @Bean
    fun fileEventsExchange(): TopicExchange {
        return ExchangeBuilder
            .topicExchange(rabbitProperties.exchanges.fileEvents)
            .durable(true)
            .build()
    }

    @Bean
    fun fileEventsDlxExchange(): TopicExchange {
        return ExchangeBuilder
            .topicExchange(rabbitProperties.exchanges.fileEventsDlx)
            .durable(true)
            .build()
    }

    // =========================
    // QUEUES
    // =========================

    @Bean
    fun fileProcessingQueue(): Queue {
        return QueueBuilder
            .durable(rabbitProperties.queues.fileProcessing)
            .withArgument("x-dead-letter-exchange", rabbitProperties.exchanges.fileEventsDlx)
            .withArgument("x-dead-letter-routing-key", "dlq.file.processing")
            .withArgument("x-message-ttl", 300000) // 5 minutos TTL
            .withArgument("x-max-retries", 3)
            .build()
    }

    @Bean
    fun fileProcessingDlq(): Queue {
        return QueueBuilder
            .durable(rabbitProperties.queues.fileProcessingDlq)
            .build()
    }

    // =========================
    // BINDINGS
    // =========================

    @Bean
    fun fileUploadedBinding(): Binding {
        return BindingBuilder
            .bind(fileProcessingQueue())
            .to(fileEventsExchange())
            .with(rabbitProperties.routing.fileUploaded)
    }

    @Bean
    fun fileProcessingStartedBinding(): Binding {
        return BindingBuilder
            .bind(fileProcessingQueue())
            .to(fileEventsExchange())
            .with(rabbitProperties.routing.fileProcessingStarted)
    }

    @Bean
    fun fileProcessingCompletedBinding(): Binding {
        return BindingBuilder
            .bind(fileProcessingQueue())
            .to(fileEventsExchange())
            .with(rabbitProperties.routing.fileProcessingCompleted)
    }

    @Bean
    fun fileProcessingFailedBinding(): Binding {
        return BindingBuilder
            .bind(fileProcessingQueue())
            .to(fileEventsExchange())
            .with(rabbitProperties.routing.fileProcessingFailed)
    }

    @Bean
    fun dlqBinding(): Binding {
        return BindingBuilder
            .bind(fileProcessingDlq())
            .to(fileEventsDlxExchange())
            .with("dlq.#")
    }
}
