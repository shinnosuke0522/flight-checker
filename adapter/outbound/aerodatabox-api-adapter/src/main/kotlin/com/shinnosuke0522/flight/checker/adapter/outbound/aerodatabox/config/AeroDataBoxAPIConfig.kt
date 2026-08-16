package com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.config

import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.api.FlightAPIClient
import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.flightinfo.FlightInfoGatewayAeroDataBoxImpl
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfoGateway
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val aeroDataBoxApiModule = module {
    single {
        val properties = get<AeroDataBoxAPIProperties>()
        FlightAPIClient(
            baseUrl = properties.baseUrl,
            httpClientEngine = CIO.create(),
            httpClientConfig = {
                it.expectSuccess = true
                it.install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            serializersModule = kotlinx.serialization.modules.SerializersModule {
                                contextual(java.time.OffsetDateTime::class, OffsetDateTimeSerializer)
                            }
                        }
                    )
                }
                it.defaultRequest {
                    header("x-rapidapi-key", properties.rapidApiKey)
                    header("x-rapidapi-host", properties.rapidApiHost)
                }
            }
        ).apply {
            setApiKey(properties.rapidApiKey)
        }
    }

    singleOf(::FlightInfoGatewayAeroDataBoxImpl) bind FlightInfoGateway::class
}
