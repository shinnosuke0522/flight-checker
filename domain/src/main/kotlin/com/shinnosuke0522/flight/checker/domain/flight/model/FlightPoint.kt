package com.shinnosuke0522.flight.checker.domain.flight.model

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import com.shinnosuke0522.flight.checker.domain.base.error.UnKnownValueError
import com.shinnosuke0522.flight.checker.domain.base.error.ValidationError
import com.shinnosuke0522.flight.checker.domain.base.error.toCause
import com.shinnosuke0522.flight.checker.domain.shared.primitive.AirportCode
import com.shinnosuke0522.flight.checker.domain.shared.primitive.CountryCode
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

data class FlightPoint(
    val countryCode: CountryCode,
    val airportCode: AirportCode,
    val zoneId: ZoneId
) {
    fun localTime(instant: Instant): LocalTime =
        Instant.from(instant).atZone(zoneId).toLocalTime()

    companion object {
        fun create(
            countryCode: String,
            airportCode: String,
            zoneId: String
        ): Either<NonEmptyList<ValidationError>, FlightPoint> = either {
            zipOrAccumulate(
                { CountryCode(value = countryCode).bind() },
                { AirportCode(value = airportCode).bind() },
                {
                    Either.catch { ZoneId.of(zoneId) }
                        .mapLeft { throwable ->
                            UnKnownValueError(
                                valueName = "ZoneId",
                                value = zoneId,
                                cause = throwable.toCause()
                            )
                        }.bind()
                }
            ) { country, airport, zoneId ->
                FlightPoint(country, airport, zoneId)
            }
        }
    }
}
