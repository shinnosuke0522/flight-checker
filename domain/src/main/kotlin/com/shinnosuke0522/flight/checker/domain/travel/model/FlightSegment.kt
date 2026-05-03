package com.shinnosuke0522.flight.checker.domain.travel.model

import arrow.core.Either
import arrow.core.raise.either
import com.shinnosuke0522.flight.checker.domain.base.error.ValidationError
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import java.time.LocalDate

data class FlightSegment(
    val identity: FlightIdentity,
    val status: FlightSegmentStatus = FlightSegmentStatus.NORMAL,
) {
    companion object {
        fun create(
            rawFlightCode: String,
            departureDate: LocalDate,
            status: FlightSegmentStatus = FlightSegmentStatus.NORMAL,
        ): Either<ValidationError, FlightSegment> = either {
            FlightSegment(
                identity = FlightIdentity.create(
                    rawFlightCode = rawFlightCode,
                    departureDate = departureDate
                ).bind(),
                status = status,
            )
        }
    }

    fun updateStatus(newStatus: FlightSegmentStatus): FlightSegment = copy(status = newStatus)
}
