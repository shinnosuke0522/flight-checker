package com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.config

object AeroDataBoxAPIConstants {
    const val PREFIX = "integration.external.api.aerodatabox"
    const val PROPERTY_ENABLED = "$PREFIX.enabled"
}

data class AeroDataBoxAPIProperties(
    val rapidApiKey: String,
    val rapidApiHost: String,
    val baseUrl: String = "https://prod.api.market/api/v1/aedbx/aerodatabox"
)
