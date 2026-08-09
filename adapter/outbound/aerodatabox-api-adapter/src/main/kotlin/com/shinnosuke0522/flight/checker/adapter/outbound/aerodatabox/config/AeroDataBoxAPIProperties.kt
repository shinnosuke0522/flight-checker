package com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.config

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault

object AeroDataBoxAPIConstants {
    const val PREFIX = "integration.external.api.aerodatabox"
    const val PROPERTY_ENABLED = "$PREFIX.enabled"
}

@ConfigMapping(prefix = AeroDataBoxAPIConstants.PREFIX)
interface AeroDataBoxAPIProperties {
    fun rapidApiKey(): String
    fun rapidApiHost(): String

    @WithDefault("true")
    fun enabled(): Boolean

    @WithDefault("https://prod.api.market/api/v1/aedbx/aerodatabox")
    fun baseUrl(): String
}
