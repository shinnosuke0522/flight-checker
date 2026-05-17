package com.shinnosuke0522.flight.checker.domain.travel.model

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.identity
import arrow.core.mapOrAccumulate
import arrow.core.raise.context.bind
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.base.model.EventSourcingAggregateRoot
import com.shinnosuke0522.flight.checker.domain.travel.error.FlightDateOutsideScheduleError
import com.shinnosuke0522.flight.checker.domain.travel.error.TravelAlreadyCanceled
import com.shinnosuke0522.flight.checker.domain.travel.error.TravelAlreadyCompleted
import com.shinnosuke0522.flight.checker.domain.travel.error.TravelBusinessRuleError
import com.shinnosuke0522.flight.checker.domain.travel.error.TravelInvariantError
import com.shinnosuke0522.flight.checker.domain.travel.event.FlightSegmentAdded
import com.shinnosuke0522.flight.checker.domain.travel.event.FlightSegmentChangeRequired
import com.shinnosuke0522.flight.checker.domain.travel.event.FlightSegmentDisrupted
import com.shinnosuke0522.flight.checker.domain.travel.event.FlightSegmentRemoved
import com.shinnosuke0522.flight.checker.domain.travel.event.TravelCanceled
import com.shinnosuke0522.flight.checker.domain.travel.event.TravelCompleted
import com.shinnosuke0522.flight.checker.domain.travel.event.TravelEvent
import com.shinnosuke0522.flight.checker.domain.travel.event.TravelPlanned
import com.shinnosuke0522.flight.checker.domain.travel.event.TravelScheduleChanged
import com.shinnosuke0522.flight.checker.domain.travel.event.TravelStarted
import java.time.Instant

@ConsistentCopyVisibility
data class Travel private constructor(
    override val id: TravelId,
    override val version: AggregateVersion,
    val name: TravelName,
    val schedule: Schedule,
    val flights: Flights,
    val status: TravelStatus,
) : EventSourcingAggregateRoot<TravelId, TravelEvent, Travel> {

    override fun apply(event: TravelEvent): Travel = when (event) {
        is TravelPlanned -> from(event)

        is TravelStarted -> Companion(
            id = id,
            version = AggregateVersion(event.sequenceNumber),
            name = name,
            schedule = schedule,
            flights = flights,
            status = TravelStatus.STARTED,
        ).fold({ error -> throw IllegalStateException(error.toString()) }, { it })

        is TravelCompleted -> Companion(
            id = id,
            version = AggregateVersion(event.sequenceNumber),
            name = name,
            schedule = schedule,
            flights = flights,
            status = TravelStatus.COMPLETED,
        ).fold({ error -> throw IllegalStateException(error.toString()) }, { it })

        is TravelCanceled -> Companion(
            id = id,
            version = AggregateVersion(event.sequenceNumber),
            name = name,
            schedule = schedule,
            flights = flights,
            status = TravelStatus.CANCELED,
        ).fold({ error -> throw IllegalStateException(error.toString()) }, { it })

        is TravelScheduleChanged -> Companion(
            id = id,
            version = AggregateVersion(event.sequenceNumber),
            name = name,
            schedule = event.newSchedule,
            flights = flights,
            status = status,
        ).fold({ error -> throw IllegalStateException(error.toString()) }, { it })

        is FlightSegmentAdded -> Companion(
            id = id,
            version = AggregateVersion(event.sequenceNumber),
            name = name,
            schedule = schedule,
            flights = flights.addFlightSegment(
                FlightSegment(identity = event.flightIdentity)
            ),
            status = status,
        ).fold({ error -> throw IllegalStateException(error.toString()) }, { it })

        is FlightSegmentRemoved -> Companion(
            id = id,
            version = AggregateVersion(event.sequenceNumber),
            name = name,
            schedule = schedule,
            flights = flights.removeFlightSegment(event.flightIdentity).getOrNull()!!,
            status = status
        ).fold({ error -> throw IllegalStateException(error.toString()) }, { it })

        is FlightSegmentDisrupted -> Companion(
            id = id,
            version = AggregateVersion(event.sequenceNumber),
            name = name,
            schedule = schedule,
            flights = flights.updateSegmentStatus(
                identity = event.flightIdentity,
                newStatus = FlightSegmentStatus.DISRUPTED
            ),
            status = status
        ).fold({ error -> throw IllegalStateException(error.toString()) }, { it })

        is FlightSegmentChangeRequired -> Companion(
            id = id,
            version = AggregateVersion(event.sequenceNumber),
            name = name,
            schedule = schedule,
            flights = flights.updateSegmentStatus(
                identity = event.flightIdentity,
                newStatus = FlightSegmentStatus.CHANGE_REQUIRED
            ),
            status = status
        ).fold({ error -> throw IllegalStateException(error.toString()) }, { it })
    }

    fun ensureUpdatable(): Either<TravelBusinessRuleError, Unit> = either {
        ensure(status != TravelStatus.CANCELED) { TravelAlreadyCanceled(id) }
        ensure(status != TravelStatus.COMPLETED) { TravelAlreadyCompleted(id) }
    }

    companion object {
        operator fun invoke(
            id: TravelId,
            version: AggregateVersion,
            name: TravelName,
            schedule: Schedule,
            flights: Flights,
            status: TravelStatus,
        ): Either<NonEmptyList<TravelInvariantError>, Travel> = either {
            verifyFlightsWithinSchedule(flights, schedule).bind()
            Travel(id, version, name, schedule, flights, status)
        }

        fun create(
            travelName: TravelName,
            schedule: Schedule,
            flights: Flights,
            createdAt: Instant
        ): Either<NonEmptyList<TravelInvariantError>, Pair<Travel, TravelPlanned>> = either {
            val event = TravelPlanned(
                id = DomainEventId.generate(),
                aggregateId = TravelId.generate(),
                sequenceNumber = AggregateVersion().nextVersion().value,
                meta = DomainEventMeta.forRootEvent { createdAt },
                name = travelName,
                schedule = schedule,
                flights = flights
            )
            val travel = from(event)
            Pair(travel, event)
        }

        fun replay(events: NonEmptyList<TravelEvent>): Travel {
            val firstEvent = events.head
            require(firstEvent is TravelPlanned) {
                "Replay must start with TravelPlanned event, but was ${firstEvent::class.simpleName}"
            }

            val initialTravel = from(firstEvent)

            return events.tail.fold(initialTravel) { travel, event ->
                travel.apply(event)
            }
        }

        internal fun verifyFlightsWithinSchedule(
            flights: Flights,
            schedule: Schedule
        ): Either<NonEmptyList<FlightDateOutsideScheduleError>, Unit> =
            flights.flightSegments.mapOrAccumulate { segment ->
                ensure(schedule.contains(segment.identity.departureDate)) {
                    FlightDateOutsideScheduleError(segment.identity.departureDate, schedule)
                }
            }.map { }

        private fun from(planedEvent: TravelPlanned): Travel = Companion(
            id = planedEvent.aggregateId,
            version = AggregateVersion(planedEvent.sequenceNumber),
            name = planedEvent.name,
            schedule = planedEvent.schedule,
            flights = planedEvent.flights,
            status = TravelStatus.PLANNED,
        ).fold({ error -> throw IllegalStateException(error.toString()) }, { it })
    }
}
