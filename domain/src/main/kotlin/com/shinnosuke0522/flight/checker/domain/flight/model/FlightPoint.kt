package com.shinnosuke0522.flight.checker.domain.flight.model

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import com.shinnosuke0522.flight.checker.domain.base.model.InvalidFormatError
import com.shinnosuke0522.flight.checker.domain.base.model.UnKnownValueError
import com.shinnosuke0522.flight.checker.domain.base.model.ValidationError
import com.shinnosuke0522.flight.checker.domain.base.model.toCause
import com.shinnosuke0522.flight.checker.domain.shared.value.CountryCode
import com.shinnosuke0522.flight.checker.domain.shared.value.AirportCode
import java.time.LocalTime
import java.time.ZoneId
import java.time.Instant

data class FlightPoint(
    val countryCode: CountryCode,
    val airportCode: AirportCode,
    val zoneId: ZoneId
) {
    fun localTime(instant: Instant) : LocalTime =
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
