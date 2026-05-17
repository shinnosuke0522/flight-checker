package com.shinnosuke0522.flight.checker.domain.travel.model

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.toNonEmptyListOrNull
import com.shinnosuke0522.flight.checker.domain.base.error.CannotBeEmptyCollectionError
import com.shinnosuke0522.flight.checker.domain.base.error.CollectionInvariantError
import com.shinnosuke0522.flight.checker.domain.base.error.ElementNotFoundError
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity

data class Flights(
    val flightSegments: NonEmptyList<FlightSegment>
) {
    internal fun addFlightSegment(newSegment: FlightSegment): Flights = copy(
        flightSegments = flightSegments + newSegment
    )

    internal fun removeFlightSegment(identity: FlightIdentity): Either<CollectionInvariantError, Flights> = either {
        ensure(flightSegments.any { it.identity == identity }) {
            ElementNotFoundError(collectionName = "flightSegments", target = identity)
        }
        val updatedList = flightSegments.filter { it.identity != identity }
        val nonEmptyUpdatedList = updatedList.toNonEmptyListOrNull()
        ensure(nonEmptyUpdatedList != null) {
            CannotBeEmptyCollectionError(collectionName = "flightSegments")
        }
        copy(flightSegments = nonEmptyUpdatedList)
    }

    internal fun updateSegmentStatus(identity: FlightIdentity, newStatus: FlightSegmentStatus): Flights =
        copy(
            flightSegments = flightSegments.map { segment ->
                if (segment.identity == identity) segment.updateStatus(newStatus) else segment
            }
        )
}
