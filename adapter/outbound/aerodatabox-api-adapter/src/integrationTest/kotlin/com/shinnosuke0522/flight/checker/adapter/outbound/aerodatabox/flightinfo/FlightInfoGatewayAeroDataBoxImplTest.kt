package com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.flightinfo

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.notFound
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.config.aeroDataBoxApiModule
import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.testfixture.config.aeroDataBoxMockModule
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfoNotExistError
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightIdentity
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import java.time.LocalDate

class FlightInfoGatewayAeroDataBoxImplTest : FunSpec(), KoinTest {

    private val sut: FlightInfoGatewayAeroDataBoxImpl by inject()
    private val wireMockServer: WireMockServer by inject()

    init {
        beforeSpec {
            startKoin {
                modules(
                    aeroDataBoxMockModule,
                    aeroDataBoxApiModule
                )
            }
        }

        afterSpec {
            wireMockServer.stop()
            stopKoin()
        }

        beforeTest {
            wireMockServer.resetAll()
        }

        context("fetchFlightInfo") {
            test("正常系: 外部APIから正常なレスポンスが返却された場合、デシリアライズしてFlightInfoを返すこと") {
                val identity = FlightIdentity.create("JL123", LocalDate.parse("2026-05-01")).shouldBeRight()

                // Atlassian OpenAPI Request Validator によって、このスタブの構造が OpenAPI と一致しているかが検証される
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

            test("異常系: 外部APIが404を返却した場合、FlightInfoNotExistErrorを返すこと") {
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
    }
}
