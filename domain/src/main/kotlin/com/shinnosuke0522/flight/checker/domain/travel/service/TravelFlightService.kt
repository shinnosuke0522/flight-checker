package com.shinnosuke0522.flight.checker.domain.travel.service

import arrow.core.Either
import arrow.core.raise.context.bind
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.travel.error.FlightDateOutsideScheduleError
import com.shinnosuke0522.flight.checker.domain.travel.error.InvalidFlightError
import com.shinnosuke0522.flight.checker.domain.travel.error.TravelError
import com.shinnosuke0522.flight.checker.domain.travel.event.FlightSegmentAdded
import com.shinnosuke0522.flight.checker.domain.travel.event.FlightSegmentChangeRequired
import com.shinnosuke0522.flight.checker.domain.travel.event.FlightSegmentDisrupted
import com.shinnosuke0522.flight.checker.domain.travel.event.FlightSegmentRemoved
import com.shinnosuke0522.flight.checker.domain.travel.event.TravelScheduleChanged
import com.shinnosuke0522.flight.checker.domain.travel.model.Schedule
import com.shinnosuke0522.flight.checker.domain.travel.model.Travel
import java.time.Instant

object TravelFlightService {
    fun changeSchedule(
        travel: Travel,
        newSchedule: Schedule,
        occurredAt: Instant
    ): Either<TravelError, Pair<Travel, TravelScheduleChanged>> = either {
        travel.ensureUpdatable().bind()

        // すべてのフライトが新しいスケジュール内にあるかチェック
        Travel.verifyFlightsWithinSchedule(travel.flights, newSchedule).mapLeft {
            InvalidFlightError(it.head.toCause())
        }.bind()

        val event = TravelScheduleChanged(
            id = DomainEventId.generate(),
            aggregateId = travel.id,
            sequenceNumber = travel.version.nextVersion().value,
            meta = DomainEventMeta.forRootEvent { occurredAt },
            newSchedule = newSchedule
        )
        val newState = travel.apply(event)
        Pair(newState, event)
    }

    fun addFlight(
        travel: Travel,
        identity: FlightIdentity,
        occurredAt: Instant
    ): Either<TravelError, Pair<Travel, FlightSegmentAdded>> = either {
        travel.ensureUpdatable().bind()

        ensure(travel.schedule.contains(identity.departureDate)) {
            FlightDateOutsideScheduleError(identity.departureDate, travel.schedule)
        }

        val event = FlightSegmentAdded(
            id = DomainEventId.generate(),
            aggregateId = travel.id,
            sequenceNumber = travel.version.nextVersion().value,
            meta = DomainEventMeta.forRootEvent { occurredAt },
            flightIdentity = identity
        )
        val newState = travel.apply(event)
        Pair(newState, event)
    }

    fun removeFlight(
        travel: Travel,
        identity: FlightIdentity,
        occurredAt: Instant
    ): Either<TravelError, Pair<Travel, FlightSegmentRemoved>> = either {
        travel.ensureUpdatable().bind()

        travel.flights.removeFlightSegment(identity).mapLeft {
            InvalidFlightError(it.toCause())
        }.bind()

        val event = FlightSegmentRemoved(
            id = DomainEventId.generate(),
            aggregateId = travel.id,
            sequenceNumber = travel.version.nextVersion().value,
            meta = DomainEventMeta.forRootEvent { occurredAt },
            flightIdentity = identity
        )
        val newState = travel.apply(event)
        Pair(newState, event)
    }

    fun markFlightDisrupted(
        travel: Travel,
        identity: FlightIdentity,
        reason: String,
        occurredAt: Instant
    ): Either<TravelError, Pair<Travel, FlightSegmentDisrupted>> = either {
        travel.ensureUpdatable().bind()

        val event = FlightSegmentDisrupted(
            id = DomainEventId.generate(),
            aggregateId = travel.id,
            sequenceNumber = travel.version.nextVersion().value,
            meta = DomainEventMeta.forRootEvent { occurredAt },
            flightIdentity = identity,
            reason = reason
        )
        val newState = travel.apply(event)
        Pair(newState, event)
    }

    fun markFlightChangeRequired(
        travel: Travel,
        identity: FlightIdentity,
        occurredAt: Instant
    ): Either<TravelError, Pair<Travel, FlightSegmentChangeRequired>> = either {
        travel.ensureUpdatable().bind()

        val event = FlightSegmentChangeRequired(
            id = DomainEventId.generate(),
            aggregateId = travel.id,
            sequenceNumber = travel.version.nextVersion().value,
            meta = DomainEventMeta.forRootEvent { occurredAt },
            flightIdentity = identity
        )
        val newState = travel.apply(event)
        Pair(newState, event)
    }
}
