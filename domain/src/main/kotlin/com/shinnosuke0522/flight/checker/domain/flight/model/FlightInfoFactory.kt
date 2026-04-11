package com.shinnosuke0522.flight.checker.domain.flight.model

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import com.shinnosuke0522.flight.checker.domain.shared.value.FlightIdentity
import java.time.Instant
import java.time.LocalDate

internal object FlightInfoFactory {
    fun createOnSchedule(
        flightCode: String,
        departureDate: LocalDate,
        departureCountryCode: String,
        departureAirportCode: String,
        departureZoneId: String,
        arrivalCountryCode: String,
        arrivalAirportCode: String,
        arrivalZoneId: String,
        scheduledDepartureTime: Instant,
        scheduledArrivalTime: Instant,
    ): Either<NonEmptyList<FlightInfoValidationError>, OnScheduleFlightInfo> = either {
        zipOrAccumulate(
            {
                createFlightIdentity(flightCode, departureDate).bind()
            },
            {
                createDeparturePoint(departureCountryCode, departureAirportCode, departureZoneId).bind()
            },
            {
                createArrivalPoint(arrivalCountryCode, arrivalAirportCode, arrivalZoneId).bind()
            }
        ) { identity, departure, arrival ->
            OnScheduleFlightInfo(
                flightIdentity = identity,
                departurePoint = departure,
                arrivalPoint = arrival,
                scheduledDepartureTime = scheduledDepartureTime,
                scheduledArrivalTime = scheduledArrivalTime
            ).bind()
        }
    }

    private fun createFlightIdentity(
        flightCode: String,
        departureDate: LocalDate
    ): Either<FlightInfoValidationError, FlightIdentity> = FlightIdentity.create(
        rawFlightCode = flightCode,
        departureDate = departureDate
    ).mapLeft {
        InvalidFlightIdentityError(it.toCause())
    }

    private fun createDeparturePoint(
        countryCode: String,
        airportCode: String,
        zoneId: String
    ): Either<NonEmptyList<FlightInfoValidationError>, FlightPoint> = FlightPoint.create(
        countryCode = countryCode,
        airportCode = airportCode,
        zoneId = zoneId
    ).mapLeft {
        it.map { error -> InvalidDeparturePoint(error.toCause()) }
    }

    private fun createArrivalPoint(
        countryCode: String,
        airportCode: String,
        zoneId: String
    ): Either<NonEmptyList<FlightInfoValidationError>, FlightPoint> = FlightPoint.create(
        countryCode = countryCode,
        airportCode = airportCode,
        zoneId = zoneId
    ).mapLeft {
        it.map { error -> InvalidArrivalPoint(error.toCause()) }
    }
}