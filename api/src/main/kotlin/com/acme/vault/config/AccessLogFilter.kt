package com.acme.vault.config

import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
@Order(-2) // Ejecutar antes de Spring Security
class AccessLogFilter : WebFilter {
    
    private val logger = LoggerFactory.getLogger(AccessLogFilter::class.java)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val startTime = System.currentTimeMillis()
        val request = exchange.request
        val timestamp = LocalDateTime.now().format(dateFormatter)
        
        // Log del request entrante
        val requestLog = buildString {
            append("HTTP_REQUEST - ")
            append("timestamp=$timestamp, ")
            append("method=${request.method}, ")
            append("uri=${request.uri}, ")
            append("path=${request.path}, ")
            append("query=${request.queryParams}, ")
            append("headers=${request.headers.toSingleValueMap()}, ")
            append("remoteAddress=${request.remoteAddress}")
        }
        
        logger.info(requestLog)
        
        return chain.filter(exchange)
            .doOnSuccess { 
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                val response = exchange.response
                val endTimestamp = LocalDateTime.now().format(dateFormatter)
                
                // Log del response
                val responseLog = buildString {
                    append("HTTP_RESPONSE - ")
                    append("timestamp=$endTimestamp, ")
                    append("method=${request.method}, ")
                    append("uri=${request.uri}, ")
                    append("status=${response.statusCode}, ")
                    append("duration=${duration}ms, ")
                    append("responseHeaders=${response.headers.toSingleValueMap()}")
                }
                
                logger.info(responseLog)
            }
            .doOnError { error ->
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                val endTimestamp = LocalDateTime.now().format(dateFormatter)
                
                // Log del error
                val errorLog = buildString {
                    append("HTTP_ERROR - ")
                    append("timestamp=$endTimestamp, ")
                    append("method=${request.method}, ")
                    append("uri=${request.uri}, ")
                    append("duration=${duration}ms, ")
                    append("error=${error.message}")
                }
                
                logger.error(errorLog, error)
            }
    }
}
