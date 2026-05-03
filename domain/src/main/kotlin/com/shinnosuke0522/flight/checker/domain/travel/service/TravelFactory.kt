package com.shinnosuke0522.flight.checker.domain.travel.service

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.right
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.travel.error.InvalidFlightError
import com.shinnosuke0522.flight.checker.domain.travel.error.InvalidTravelNameError
import com.shinnosuke0522.flight.checker.domain.travel.error.TravelValidationError
import com.shinnosuke0522.flight.checker.domain.travel.event.TravelPlanned
import com.shinnosuke0522.flight.checker.domain.travel.model.Flights
import com.shinnosuke0522.flight.checker.domain.travel.model.OneWayTripSchedule
import com.shinnosuke0522.flight.checker.domain.travel.model.RoundTripSchedule
import com.shinnosuke0522.flight.checker.domain.travel.model.Schedule
import com.shinnosuke0522.flight.checker.domain.travel.model.TravelId
import com.shinnosuke0522.flight.checker.domain.travel.model.TravelName
import java.time.LocalDate

object TravelFactory {
    fun execute(
        command: TravelPlaneCommand
    ): Either<NonEmptyList<TravelValidationError>, TravelPlanned> = either {
        val travelName = createTravelName(command.rawTravelName).bind()
        val schedule = createSchedule(command.departureDate, command.returnDate).bind()
        val flights = createFlights(command.rawFlightSegments).bind()

        TravelPlanned(
            id = DomainEventId.generate(),
            aggregateId = TravelId.generate(),
            sequenceNumber = AggregateVersion.Companion().nextVersion().value,
            meta = DomainEventMeta.forRootEvent { command.createdAt },
            name = travelName,
            schedule = schedule,
            flights = flights
        )
    }

    private fun createTravelName(rawTravelName: String) =
        TravelName.Companion(rawTravelName)
            .mapLeft { nonEmptyListOf(InvalidTravelNameError(it.toCause())) }

    private fun createSchedule(
        departureDate: LocalDate,
        returnDate: LocalDate?
    ): Either<NonEmptyList<TravelValidationError>, Schedule> =
        if (returnDate == null) {
            OneWayTripSchedule(departureDate).right()
        } else {
            RoundTripSchedule.Companion(departureDate, returnDate)
                .mapLeft { nonEmptyListOf(it) }
        }

    private fun createFlights(
        rawFlightSegments: NonEmptyList<Pair<String, LocalDate>>
    ): Either<NonEmptyList<TravelValidationError>, Flights> =
        Flights.create(rawFlightSegments)
            .mapLeft { errors ->
                errors.map { InvalidFlightError(it.toCause()) }
            }
}