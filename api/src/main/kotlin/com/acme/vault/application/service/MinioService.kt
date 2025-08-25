package com.acme.vault.application.service

import com.acme.vault.config.properties.MinioProperties
import io.minio.*
import io.minio.errors.*
import io.minio.http.Method
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.InputStream
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.time.Duration
import java.util.concurrent.TimeUnit

@Service
class MinioService(
    private val minioClient: MinioClient,
    private val minioProperties: MinioProperties
) {

    companion object {
        private const val DEFAULT_EXPIRY_TIME = 7 // days
    }

    init {
        // Crear bucket si no existe al inicializar el servicio
        createBucketIfNotExists().subscribe()
    }

    /**
     * Crea el bucket principal si no existe
     */
    fun createBucketIfNotExists(): Mono<Void> {
        return Mono.fromCallable {
            try {
                val bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                        .bucket(minioProperties.bucketName)
                        .build()
                )

                if (!bucketExists) {
                    println("=== MINIO SERVICE: Creating bucket: ${minioProperties.bucketName} ===")
                    minioClient.makeBucket(
                        MakeBucketArgs.builder()
                            .bucket(minioProperties.bucketName)
                            .build()
                    )
                    println("Bucket created successfully: ${minioProperties.bucketName}")
                } else {
                    println("=== MINIO SERVICE: Bucket already exists: ${minioProperties.bucketName} ===")
                }
            } catch (e: Exception) {
                println("Error creating bucket: ${e.message}")
                throw RuntimeException("Failed to create MinIO bucket", e)
            }
        }
            .subscribeOn(Schedulers.boundedElastic())
            .then()
    }

    /**
     * Sube un archivo a MinIO
     */
    fun uploadFile(
        inputStream: InputStream,
        objectName: String,
        contentType: String,
        size: Long = -1
    ): Mono<String> {
        return Mono.fromCallable {
            try {
                println("=== MINIO SERVICE: Uploading file: $objectName ===")
                
                val putObjectArgs = PutObjectArgs.builder()
                    .bucket(minioProperties.bucketName)
                    .`object`(objectName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build()

                val result = minioClient.putObject(putObjectArgs)
                println("File uploaded successfully: $objectName, ETag: ${result.etag()}")
                
                objectName // Retornar el nombre del objeto como identificador
            } catch (e: Exception) {
                println("Error uploading file: ${e.message}")
                throw RuntimeException("Failed to upload file to MinIO", e)
            } finally {
                try {
                    inputStream.close()
                } catch (e: Exception) {
                    println("Warning: Failed to close input stream: ${e.message}")
                }
            }
        }
            .subscribeOn(Schedulers.boundedElastic())
    }

    /**
     * Descarga un archivo de MinIO
     */
    fun downloadFile(objectName: String): Mono<InputStream> {
        return Mono.fromCallable {
            try {
                println("=== MINIO SERVICE: Downloading file: $objectName ===")

                minioClient.getObject(
                    GetObjectArgs.builder()
                        .bucket(minioProperties.bucketName)
                        .`object`(objectName)
                        .build()
                ) as InputStream // Cast explícito a InputStream
            } catch (e: Exception) {
                println("Error downloading file: ${e.message}")
                throw RuntimeException("Failed to download file from MinIO", e)
            }
        }
            .subscribeOn(Schedulers.boundedElastic())
    }

    /**
     * Elimina un archivo de MinIO
     */
    fun deleteFile(objectName: String): Mono<Boolean> {
        return Mono.fromCallable {
            try {
                println("=== MINIO SERVICE: Deleting file: $objectName ===")
                
                minioClient.removeObject(
                    RemoveObjectArgs.builder()
                        .bucket(minioProperties.bucketName)
                        .`object`(objectName)
                        .build()
                )
                
                println("File deleted successfully: $objectName")
                true
            } catch (e: Exception) {
                println("Error deleting file: ${e.message}")
                false
            }
        }
            .subscribeOn(Schedulers.boundedElastic())
    }

    /**
     * Genera una URL presignada para descarga temporal
     */
    fun generatePresignedDownloadUrl(
        objectName: String,
        expiry: Duration = Duration.ofDays(DEFAULT_EXPIRY_TIME.toLong())
    ): Mono<String> {
        return Mono.fromCallable {
            try {
                println("=== MINIO SERVICE: Generating presigned URL for: $objectName ===")
                
                val url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(minioProperties.bucketName)
                        .`object`(objectName)
                        .expiry(expiry.seconds.toInt(), TimeUnit.SECONDS)
                        .build()
                )
                
                println("Presigned URL generated successfully for: $objectName")
                url
            } catch (e: Exception) {
                println("Error generating presigned URL: ${e.message}")
                throw RuntimeException("Failed to generate presigned URL", e)
            }
        }
            .subscribeOn(Schedulers.boundedElastic())
    }

    /**
     * Genera una URL presignada para upload temporal
     */
    fun generatePresignedUploadUrl(
        objectName: String,
        expiry: Duration = Duration.ofHours(1)
    ): Mono<String> {
        return Mono.fromCallable {
            try {
                println("=== MINIO SERVICE: Generating presigned upload URL for: $objectName ===")
                
                val url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                        .method(Method.PUT)
                        .bucket(minioProperties.bucketName)
                        .`object`(objectName)
                        .expiry(expiry.seconds.toInt(), TimeUnit.SECONDS)
                        .build()
                )
                
                println("Presigned upload URL generated successfully for: $objectName")
                url
            } catch (e: Exception) {
                println("Error generating presigned upload URL: ${e.message}")
                throw RuntimeException("Failed to generate presigned upload URL", e)
            }
        }
            .subscribeOn(Schedulers.boundedElastic())
    }

    /**
     * Verifica si un archivo existe en MinIO
     */
    fun fileExists(objectName: String): Mono<Boolean> {
        return Mono.fromCallable {
            try {
                minioClient.statObject(
                    StatObjectArgs.builder()
                        .bucket(minioProperties.bucketName)
                        .`object`(objectName)
                        .build()
                )
                true
            } catch (e: ErrorResponseException) {
                if (e.errorResponse().code() == "NoSuchKey") {
                    false
                } else {
                    throw RuntimeException("Error checking file existence", e)
                }
            } catch (e: Exception) {
                throw RuntimeException("Error checking file existence", e)
            }
        }
            .subscribeOn(Schedulers.boundedElastic())
    }

    /**
     * Obtiene información de un objeto
     */
    fun getObjectInfo(objectName: String): Mono<StatObjectResponse> {
        return Mono.fromCallable {
            try {
                minioClient.statObject(
                    StatObjectArgs.builder()
                        .bucket(minioProperties.bucketName)
                        .`object`(objectName)
                        .build()
                )
            } catch (e: Exception) {
                throw RuntimeException("Failed to get object info", e)
            }
        }
            .subscribeOn(Schedulers.boundedElastic())
    }
}
