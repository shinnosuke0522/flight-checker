package com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.context.properties.ConfigurationProperties

object AeroDataBoxAPIConstants {
    const val PREFIX = "integration.external.api.aerodatabox"
    const val PROPERTY_ENABLED = "$PREFIX.enabled"
}

@ConditionalOnBooleanProperty(
    value = [AeroDataBoxAPIConstants.PROPERTY_ENABLED],
    havingValue = true,
    matchIfMissing = false
)
@ConfigurationProperties(prefix = AeroDataBoxAPIConstants.PREFIX)
data class AeroDataBoxAPIProperties(
    val rapidApiKey: String,
    val rapidApiHost: String
)
