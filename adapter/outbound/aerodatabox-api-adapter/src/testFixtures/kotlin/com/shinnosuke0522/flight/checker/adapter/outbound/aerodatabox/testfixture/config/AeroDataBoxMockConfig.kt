package com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.testfixture.config

import com.atlassian.oai.validator.wiremock.OpenApiValidationListener
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.config.AeroDataBoxAPIProperties
import org.koin.dsl.module

val aeroDataBoxMockModule = module {
    single {
        val server = WireMockServer(options().dynamicPort())
        val validationListener = OpenApiValidationListener(
            "src/main/resources/contract/aerodatabox-api-v1.15.1.0.yaml"
        )
        server.addMockServiceRequestListener(validationListener)
        server.start()
        server
    }

    single {
        val server = get<WireMockServer>()
        AeroDataBoxAPIProperties(
            rapidApiKey = "test-key",
            rapidApiHost = "test-host",
            baseUrl = server.baseUrl()
        )
    }
}
