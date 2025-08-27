package com.acme.vault.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app.rabbitmq")
data class RabbitProperties(
    var host: String = "localhost",
    var port: Int = 5672,
    var username: String = "guest",
    var password: String = "guest",
    var virtualHost: String = "/",
    var exchanges: ExchangeProperties = ExchangeProperties(),
    var queues: QueueProperties = QueueProperties(),
    var routing: RoutingProperties = RoutingProperties()
) {
    data class ExchangeProperties(
        var fileEvents: String = "file.events",
        var fileEventsDlx: String = "file.events.dlx"
    )

    data class QueueProperties(
        var fileProcessing: String = "file.processing",
        var fileProcessingDlq: String = "file.processing.dlq"
    )

    data class RoutingProperties(
        var fileUploaded: String = "file.uploaded",
        var fileProcessingStarted: String = "file.processing.started",
        var fileProcessingCompleted: String = "file.processing.completed", 
        var fileProcessingFailed: String = "file.processing.failed"
    )
}
