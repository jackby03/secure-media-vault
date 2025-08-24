package com.acme.vault.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@Configuration
@EnableR2dbcRepositories(basePackages = ["com.acme.vault.adapter.out.persistance"])
class R2dbcConfig
