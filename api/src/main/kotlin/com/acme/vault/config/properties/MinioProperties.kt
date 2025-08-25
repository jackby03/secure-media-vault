package com.acme.vault.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "minio")
data class MinioProperties @ConstructorBinding constructor(
    val url: String,
    val accessKey: String,
    val secretKey: String,
    val bucketName: String = "secure-media-vault",
    val connectTimeout: Int = 10000, // 10 seconds
    val writeTimeout: Int = 60000,   // 60 seconds
    val readTimeout: Int = 10000     // 10 seconds
)
