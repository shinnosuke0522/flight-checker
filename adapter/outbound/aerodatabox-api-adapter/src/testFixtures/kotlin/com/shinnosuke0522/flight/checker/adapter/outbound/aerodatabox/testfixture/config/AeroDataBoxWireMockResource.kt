package com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.testfixture.config

import com.atlassian.oai.validator.wiremock.OpenApiValidationListener
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager

class AeroDataBoxWireMockResource : QuarkusTestResourceLifecycleManager {

    private lateinit var wireMockServer: WireMockServer

    override fun start(): Map<String, String> {
        wireMockServer = WireMockServer(options().dynamicPort())
        val validationListener = OpenApiValidationListener(
            "src/main/resources/contract/aerodatabox-api-v1.15.1.0.yaml"
        )
        wireMockServer.addMockServiceRequestListener(validationListener)
        wireMockServer.start()

        return mapOf(
            "integration.external.api.aerodatabox.base-url" to wireMockServer.baseUrl()
        )
    }

    override fun stop() {
        if (::wireMockServer.isInitialized) {
            wireMockServer.stop()
        }
    }

    override fun inject(testInstance: Any) {
        val type = testInstance::class.java
        type.declaredFields.firstOrNull { it.type == WireMockServer::class.java }?.let { field ->
            field.isAccessible = true
            field.set(testInstance, wireMockServer)
        }
    }
}
