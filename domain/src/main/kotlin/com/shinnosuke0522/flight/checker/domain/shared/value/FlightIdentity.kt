package com.shinnosuke0522.flight.checker.domain.shared.value

import arrow.core.Either
import arrow.core.raise.either
import com.shinnosuke0522.flight.checker.domain.base.error.ValidationError
import java.time.LocalDate

data class FlightIdentity(
    val flightCode: FlightCode,
    val departureDate: LocalDate
) {
    companion object {
        fun create(
            rawFlightCode: String,
            departureDate: LocalDate,
        ): Either<ValidationError, FlightIdentity> = either {
            val flightCode = FlightCode(value = rawFlightCode).bind()
            FlightIdentity(flightCode, departureDate)
        }
    }
}
