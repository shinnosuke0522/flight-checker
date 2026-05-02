package com.shinnosuke0522.flight.checker.domain.travel.model

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import arrow.core.right
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateRoot
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import java.time.LocalDate

data class Travel(
    override val id: TravelId,
    override val version: AggregateVersion = AggregateVersion(),
    val name: TravelName,
    val schedule: Schedule,
    val flights: Flights,
    val status: TravelStatus = TravelStatus.PLANNED,
) : AggregateRoot<TravelId> {
    companion object {
        fun create(
            rawTravelName: String,
            departureDate: LocalDate,
            returnDate: LocalDate? = null,
            rawFlightSegments: NonEmptyList<Pair<String, LocalDate>>
        ): Either<NonEmptyList<TravelValidationError>, Travel> = TravelFactory.create(
            rawTravelName = rawTravelName,
            departureDate = departureDate,
            returnDate = returnDate,
            rawFlightSegments = rawFlightSegments
        )
    }
}

enum class TravelStatus {
    PLANNED,
    ONGOING,
    COMPLETED,
    CANCELED,
}

private object TravelFactory {
    fun create(
        rawTravelName: String,
        departureDate: LocalDate,
        returnDate: LocalDate? = null,
        rawFlightSegments: NonEmptyList<Pair<String, LocalDate>>
    ): Either<NonEmptyList<TravelValidationError>, Travel> = either {
        zipOrAccumulate(
            { createTravelName(rawTravelName).bind() },
            { createSchedule(departureDate, returnDate).bind() },
            { createFlights(rawFlightSegments).bind() }
        ) { travelName, schedule, flightSegments ->
            Travel(
                id = TravelId.generate(),
                name = travelName,
                schedule = schedule,
                flights = flightSegments,
                status = TravelStatus.PLANNED,
            )
        }
    }

    private fun createTravelName(rawTravelName: String) =
        TravelName(rawTravelName)
            .mapLeft { InvalidTravelNameError(it.toCause()) }

    private fun createSchedule(
        departureDate: LocalDate,
        returnDate: LocalDate?
    ): Either<TravelValidationError, Schedule> =
        if (returnDate == null) {
            OneWayTripSchedule(departureDate).right()
        } else {
            RoundTripSchedule(departureDate, returnDate)
        }

    private fun createFlights(
        rawFlightSegments: NonEmptyList<Pair<String, LocalDate>>
    ): Either<NonEmptyList<TravelValidationError>, Flights> =
        Flights.create(rawFlightSegments)
            .mapLeft { errors ->
                errors.map { InvalidFlightError(it.toCause()) }
            }
}
