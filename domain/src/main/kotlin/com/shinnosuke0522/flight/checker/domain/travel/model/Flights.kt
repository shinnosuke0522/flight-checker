package com.shinnosuke0522.flight.checker.domain.travel.model

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.mapOrAccumulate
import arrow.core.toNonEmptyListOrNull
import com.shinnosuke0522.flight.checker.domain.base.model.ValidationError
import java.time.LocalDate

data class Flights(
    val flightSegments: NonEmptyList<FlightSegment>
) {
    companion object {
        fun create(
            rawFlightSegments: NonEmptyList<Pair<String, LocalDate>>
        ): Either<NonEmptyList<ValidationError>, Flights> =
            rawFlightSegments.mapOrAccumulate { (rawFlightCode, departureDate) ->
                FlightSegment.create(
                    rawFlightCode = rawFlightCode,
                    departureDate = departureDate
                ).bind()
            }.map { Flights(it.toNonEmptyListOrNull()!!) }
    }
}
