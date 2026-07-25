package com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.config

import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.api.FlightAPIClient
import okhttp3.OkHttpClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(
    value = [AeroDataBoxAPIConstants.PROPERTY_ENABLED],
    havingValue = "true",
    matchIfMissing = false
)
class AeroDataBoxAPIConfig(
    private val properties: AeroDataBoxAPIProperties
) {
    @Bean
    fun aeroDataBoxOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("x-rapidapi-key", properties.rapidApiKey)
                    .addHeader("x-rapidapi-host", properties.rapidApiHost)
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Bean
    fun flightAPIClient(aeroDataBoxOkHttpClient: OkHttpClient): FlightAPIClient {
        return FlightAPIClient(
            basePath = properties.baseUrl,
            client = aeroDataBoxOkHttpClient
        )
    }
}
