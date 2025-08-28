package com.acme.vault.adapter.web.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        
        val errorResponse = when {
            ex.message?.contains("cache", ignoreCase = true) == true -> {
                println("Cache error handled gracefully: ${ex.message}")
                ErrorResponse(
                    timestamp = LocalDateTime.now(),
                    status = HttpStatus.SERVICE_UNAVAILABLE.value(),
                    error = "Cache Service Temporarily Unavailable",
                    message = "The cache service is temporarily unavailable. Data will be retrieved from the primary source.",
                    path = exchange.request.path.toString()
                )
            }
            ex.message?.contains("redis", ignoreCase = true) == true -> {
                println("Redis error handled gracefully: ${ex.message}")
                ErrorResponse(
                    timestamp = LocalDateTime.now(),
                    status = HttpStatus.SERVICE_UNAVAILABLE.value(),
                    error = "Cache Service Error",
                    message = "Cache service is experiencing issues. Functionality may be slower but will continue to work.",
                    path = exchange.request.path.toString()
                )
            }
            else -> {
                println("Generic error handled: ${ex.message}")
                ErrorResponse(
                    timestamp = LocalDateTime.now(),
                    status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    error = "Internal Server Error",
                    message = "An unexpected error occurred. Please try again later.",
                    path = exchange.request.path.toString()
                )
            }
        }

        return Mono.just(
            ResponseEntity
                .status(errorResponse.status)
                .body(errorResponse)
        )
    }

    @ExceptionHandler(org.springframework.data.redis.RedisConnectionFailureException::class)
    fun handleRedisConnectionFailure(
        ex: org.springframework.data.redis.RedisConnectionFailureException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        
        println("Redis connection failure handled: ${ex.message}")
        
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.SERVICE_UNAVAILABLE.value(),
            error = "Cache Service Unavailable",
            message = "Cache service is currently unavailable. The application will continue to function without caching.",
            path = exchange.request.path.toString()
        )

        return Mono.just(
            ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorResponse)
        )
    }
}

data class ErrorResponse(
    val timestamp: LocalDateTime,
    val status: Int,
    val error: String,
    val message: String,
    val path: String
)
