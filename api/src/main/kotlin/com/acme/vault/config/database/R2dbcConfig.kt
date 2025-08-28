package com.acme.vault.config.database

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.spi.ConnectionFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.transaction.ReactiveTransactionManager
import java.time.Duration

@Configuration
@EnableR2dbcRepositories(basePackages = ["com.acme.vault.adapter.persistance"])
class R2dbcConfig : AbstractR2dbcConfiguration() {

    @Value("\${spring.r2dbc.username}")
    private lateinit var username: String

    @Value("\${spring.r2dbc.password}")
    private lateinit var password: String

    @Value("\${spring.r2dbc.host:localhost}")
    private lateinit var host: String

    @Value("\${spring.r2dbc.port:5432}")
    private var port: Int = 5432

    @Value("\${spring.r2dbc.database}")
    private lateinit var database: String

    @Bean
    override fun connectionFactory(): ConnectionFactory {
        return PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host(host)
                .port(port)
                .database(database)
                .username(username)
                .password(password)
                .connectTimeout(Duration.ofSeconds(10))
                .lockWaitTimeout(Duration.ofSeconds(30))
                .statementTimeout(Duration.ofSeconds(60))
                .tcpKeepAlive(true)
                .tcpNoDelay(true)
                .preparedStatementCacheQueries(256)
                .applicationName("secure-media-vault")
                .build()
        )
    }

    @Bean
    fun transactionManager(connectionFactory: ConnectionFactory): ReactiveTransactionManager {
        return R2dbcTransactionManager(connectionFactory)
    }
}
