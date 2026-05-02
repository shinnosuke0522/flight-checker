package com.shinnosuke0522.flight.checker.domain.travel.model

import arrow.core.Either
import arrow.core.raise.either
import com.shinnosuke0522.flight.checker.domain.base.error.ValidationError
import com.shinnosuke0522.flight.checker.domain.shared.value.FlightIdentity
import java.time.LocalDate

data class FlightSegment (
    val identity: FlightIdentity,
) {
    companion object {
        fun create(
            rawFlightCode: String,
            departureDate: LocalDate
        ): Either<ValidationError, FlightSegment> = either {
            FlightSegment(
                identity = FlightIdentity.create(
                    rawFlightCode = rawFlightCode,
                    departureDate = departureDate
                ).bind()
            )
        }
    }
}