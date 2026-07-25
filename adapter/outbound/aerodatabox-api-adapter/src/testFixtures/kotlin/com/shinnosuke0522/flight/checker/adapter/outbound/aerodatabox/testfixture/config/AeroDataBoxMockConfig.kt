package com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.testfixture.config

import com.atlassian.oai.validator.wiremock.OpenApiValidationListener
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.config.AeroDataBoxAPIConstants
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistrar

@TestConfiguration
open class AeroDataBoxMockConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    open fun wireMockServer(): WireMockServer {
        val server = WireMockServer(options().dynamicPort())
        val validationListener = OpenApiValidationListener(
            "src/main/resources/contract/aerodatabox-api-v1.15.1.0.yaml"
        )
        server.addMockServiceRequestListener(validationListener)
        return server
    }

    @Bean
    open fun wireMockProperties(wireMockServer: WireMockServer): DynamicPropertyRegistrar {
        return DynamicPropertyRegistrar { registry ->
            registry.add("${AeroDataBoxAPIConstants.PREFIX}.base-url") {
                wireMockServer.baseUrl()
            }
        }
    }
}
