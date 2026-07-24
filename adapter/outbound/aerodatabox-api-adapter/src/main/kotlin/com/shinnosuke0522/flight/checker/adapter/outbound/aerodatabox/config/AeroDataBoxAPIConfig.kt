package com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties
data class AeroDataBoxAPIConfig(
    val rapidApiKey: String
)
