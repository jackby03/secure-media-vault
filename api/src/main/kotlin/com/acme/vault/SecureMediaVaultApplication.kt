package com.acme.vault

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class SecureMediaVaultApplication

fun main(args: Array<String>) {
    runApplication<SecureMediaVaultApplication>(*args)
}
