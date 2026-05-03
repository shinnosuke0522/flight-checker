package com.shinnosuke0522.flight.checker.domain.shared.primitive

import arrow.core.Either
import arrow.core.raise.either
import com.shinnosuke0522.flight.checker.domain.base.error.ValidationError
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
        ): Either<ValidationError, FlightIdentity> = either {
            val flightCode = FlightCode(value = rawFlightCode).bind()
            FlightIdentity(flightCode, departureDate)
        }
    }

}
