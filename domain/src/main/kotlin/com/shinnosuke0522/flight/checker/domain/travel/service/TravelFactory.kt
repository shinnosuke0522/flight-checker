package com.shinnosuke0522.flight.checker.domain.travel.service

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.mapOrAccumulate
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import arrow.core.right
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.travel.error.InvalidFlightError
import com.shinnosuke0522.flight.checker.domain.travel.error.InvalidTravelNameError
import com.shinnosuke0522.flight.checker.domain.travel.error.TravelInvariantError
import com.shinnosuke0522.flight.checker.domain.travel.event.TravelPlanned
import com.shinnosuke0522.flight.checker.domain.travel.model.FlightSegment
import com.shinnosuke0522.flight.checker.domain.travel.model.Flights
import com.shinnosuke0522.flight.checker.domain.travel.model.OneWayTripSchedule
import com.shinnosuke0522.flight.checker.domain.travel.model.RoundTripSchedule
import com.shinnosuke0522.flight.checker.domain.travel.model.Schedule
import com.shinnosuke0522.flight.checker.domain.travel.model.Travel
import com.shinnosuke0522.flight.checker.domain.travel.model.TravelName
import java.time.LocalDate

object TravelFactory {
    fun execute(
        command: TravelPlaneCommand
    ): Either<NonEmptyList<TravelInvariantError>, Pair<Travel, TravelPlanned>> = either {
        zipOrAccumulate(
            { createTravelName(command.rawTravelName).bind() },
            { createSchedule(command.departureDate, command.returnDate).bind() },
            { createFlights(command.rawFlightSegments).bind() }
        ) { travelName, schedule, flights ->
            Travel.create(
                travelName = travelName,
                schedule = schedule,
                flights = flights,
                createdAt = command.createdAt
            ).bind()
        }
    }

    private fun createTravelName(rawTravelName: String) =
        TravelName.Companion(rawTravelName)
            .mapLeft { nonEmptyListOf(InvalidTravelNameError(it.toCause())) }

    private fun createSchedule(
        departureDate: LocalDate,
        returnDate: LocalDate?
    ): Either<NonEmptyList<TravelInvariantError>, Schedule> =
        if (returnDate == null) {
            OneWayTripSchedule(departureDate).right()
        } else {
            RoundTripSchedule.Companion(departureDate, returnDate)
                .mapLeft { nonEmptyListOf(it) }
        }

    private fun createFlights(
        rawFlightSegments: NonEmptyList<Pair<String, LocalDate>>
    ): Either<NonEmptyList<TravelInvariantError>, Flights> = either {
        val segments = rawFlightSegments.mapOrAccumulate { (rawFlightCode, departureDate) ->
            val identity = FlightIdentity.create(
                rawFlightCode = rawFlightCode,
                departureDate = departureDate
            ).bind()
            FlightSegment(identity)
        }.mapLeft { errors ->
            errors.map { InvalidFlightError(it.toCause()) }
        }.bind()

        Flights(segments)
    }
}
