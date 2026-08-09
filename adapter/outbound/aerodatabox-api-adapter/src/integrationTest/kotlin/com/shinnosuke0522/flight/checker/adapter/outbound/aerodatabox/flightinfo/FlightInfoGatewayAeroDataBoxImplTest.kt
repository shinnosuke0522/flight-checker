package com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.flightinfo

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.notFound
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.testfixture.config.AeroDataBoxWireMockResource
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfoNotExistError
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightIdentity
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AeroDataBoxTestProfile : QuarkusTestProfile {
    override fun getConfigOverrides(): Map<String, String> {
        return mapOf(
            "integration.external.api.aerodatabox.rapid-api-key" to "test-key",
            "integration.external.api.aerodatabox.rapid-api-host" to "test-host",
            "integration.external.api.aerodatabox.enabled" to "true"
        )
    }
}

@QuarkusTest
@TestProfile(AeroDataBoxTestProfile::class)
@QuarkusTestResource(AeroDataBoxWireMockResource::class)
class FlightInfoGatewayAeroDataBoxImplTest {

    @Inject
    lateinit var sut: FlightInfoGatewayAeroDataBoxImpl

    lateinit var wireMockServer: WireMockServer

    @BeforeEach
    fun setup() {
        wireMockServer.resetAll()
    }

    @Test
    fun `正常系 外部APIから正常なレスポンスが返却された場合、デシリアライズしてFlightInfoを返すこと`() = runBlocking<Unit> {
        val identity = FlightIdentity.create("JL123", LocalDate.parse("2026-05-01")).shouldBeRight()

        wireMockServer.stubFor(
            get(urlPathEqualTo("/flights/Number/JL123/2026-05-01T00:00Z"))
                .withHeader("x-rapidapi-key", equalTo("test-key"))
                .willReturn(
                    okJson(
                        this::class.java.getResource(
                            "/test-case/flight-info-gateway/succeeded/flight-jl123.json"
                        )!!.readText()
                    )
                )
        )

        val result = sut.fetchFlightInfo(identity)

        val flightInfo = result.shouldBeRight()
        flightInfo.id shouldBe identity
    }

    @Test
    fun `異常系 外部APIが404を返却した場合、FlightInfoNotExistErrorを返すこと`() = runBlocking<Unit> {
        val identity = FlightIdentity.create("JL999", LocalDate.parse("2026-05-01")).shouldBeRight()

        wireMockServer.stubFor(
            get(urlPathEqualTo("/flights/Number/JL999/2026-05-01T00:00Z"))
                .willReturn(notFound())
        )

        val result = sut.fetchFlightInfo(identity)

        result.isLeft() shouldBe true
        result.shouldBeLeft().shouldBeInstanceOf<FlightInfoNotExistError>()
    }
}
