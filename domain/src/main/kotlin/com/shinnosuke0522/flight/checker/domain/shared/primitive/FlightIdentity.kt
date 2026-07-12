package com.shinnosuke0522.flight.checker.domain.shared.primitive

import arrow.core.Either
import arrow.core.raise.either
import com.shinnosuke0522.flight.checker.domain.base.error.InvariantError
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateId
import java.time.LocalDate

@ConsistentCopyVisibility
data class FlightIdentity private constructor(
    val flightCode: FlightCode,
    val departureDate: LocalDate
) : AggregateId {
    override fun asString(): String = "${flightCode.value}-$departureDate"

    companion object {
        fun create(
            rawFlightCode: String,
            departureDate: LocalDate,
        ): Either<InvariantError, FlightIdentity> = either {
            val flightCode = FlightCode(value = rawFlightCode).bind()
            FlightIdentity(flightCode, departureDate)
        }

        fun fromString(value: String): Either<InvariantError, FlightIdentity> = either {
            val dashIndex = value.lastIndexOf('-')
            val flightCodeStr = value.substring(0, dashIndex)
            val dateStr = value.substring(dashIndex + 1)
            create(flightCodeStr, LocalDate.parse(dateStr)).bind()
        }
    }
}
