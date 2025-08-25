package com.acme.vault.config.storage

import com.acme.vault.config.properties.MinioProperties
import io.minio.MinioClient
import okhttp3.OkHttpClient
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableConfigurationProperties(MinioProperties::class)
class MinioConfig(
    private val minioProperties: MinioProperties
) {

    @Bean
    fun minioClient(): MinioClient {
        println("=== MINIO CONFIG: Configuring MinIO client with URL: ${minioProperties.url} ===")
        
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(minioProperties.connectTimeout.toLong(), TimeUnit.MILLISECONDS)
            .writeTimeout(minioProperties.writeTimeout.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(minioProperties.readTimeout.toLong(), TimeUnit.MILLISECONDS)
            .build()

        return MinioClient.builder()
            .endpoint(minioProperties.url)
            .credentials(minioProperties.accessKey, minioProperties.secretKey)
            .httpClient(httpClient)
            .build()
            .also {
                println("MinIO client configured successfully")
            }
    }
}
