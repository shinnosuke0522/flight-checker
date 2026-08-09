package com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.config

import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.api.FlightAPIClient
import io.quarkus.arc.properties.IfBuildProperty
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import okhttp3.OkHttpClient

@ApplicationScoped
@IfBuildProperty(
    name = AeroDataBoxAPIConstants.PROPERTY_ENABLED,
    stringValue = "true"
)
class AeroDataBoxAPIConfig(
    private val properties: AeroDataBoxAPIProperties
) {
    @Produces
    @ApplicationScoped
    fun aeroDataBoxOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("x-rapidapi-key", properties.rapidApiKey())
                    .addHeader("x-rapidapi-host", properties.rapidApiHost())
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Produces
    @ApplicationScoped
    fun flightAPIClient(aeroDataBoxOkHttpClient: OkHttpClient): FlightAPIClient {
        return FlightAPIClient(
            basePath = properties.baseUrl(),
            client = aeroDataBoxOkHttpClient
        )
    }
}
