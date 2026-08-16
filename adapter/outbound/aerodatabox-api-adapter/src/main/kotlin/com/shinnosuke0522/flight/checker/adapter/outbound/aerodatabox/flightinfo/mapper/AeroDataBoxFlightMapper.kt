package com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.flightinfo.mapper

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.model.FlightContract
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfo
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfoInvalidDataError
import com.shinnosuke0522.flight.checker.domain.flight.info.model.ScheduledFlightInfo
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightPoint

object AeroDataBoxFlightMapper {
    fun toDomain(
        flight: FlightContract,
        identity: FlightIdentity
    ): Either<FlightInfoInvalidDataError, FlightInfo> = either {
        val departureAirport = flight.departure.airport
        val arrivalAirport = flight.arrival.airport

        val scheduledDepartureTime = flight.departure.scheduledTime?.utc
        val scheduledArrivalTime = flight.arrival.scheduledTime?.utc

        ensureNotNull(scheduledDepartureTime) {
            FlightInfoInvalidDataError(IllegalArgumentException("Scheduled departure time is missing"))
        }
        ensureNotNull(scheduledArrivalTime) {
            FlightInfoInvalidDataError(IllegalArgumentException("Scheduled arrival time is missing"))
        }
        ensureNotNull(departureAirport.countryCode) {
            FlightInfoInvalidDataError(IllegalArgumentException("Departure country code is missing"))
        }
        ensureNotNull(arrivalAirport.countryCode) {
            FlightInfoInvalidDataError(IllegalArgumentException("Arrival country code is missing"))
        }
        ensureNotNull(departureAirport.timeZone) {
            FlightInfoInvalidDataError(IllegalArgumentException("Departure timezone is missing"))
        }
        ensureNotNull(arrivalAirport.timeZone) {
            FlightInfoInvalidDataError(IllegalArgumentException("Arrival timezone is missing"))
        }

        val depIata = departureAirport.iata ?: departureAirport.icao
        val arrIata = arrivalAirport.iata ?: arrivalAirport.icao

        ensureNotNull(depIata) {
            FlightInfoInvalidDataError(IllegalArgumentException("Departure IATA/ICAO is missing"))
        }
        ensureNotNull(arrIata) {
            FlightInfoInvalidDataError(IllegalArgumentException("Arrival IATA/ICAO is missing"))
        }

        val departurePoint = FlightPoint.create(
            countryCode = departureAirport.countryCode,
            airportCode = depIata,
            zoneId = departureAirport.timeZone
        ).mapLeft { FlightInfoInvalidDataError(IllegalStateException(it.head.message)) }.bind()

        val arrivalPoint = FlightPoint.create(
            countryCode = arrivalAirport.countryCode,
            airportCode = arrIata,
            zoneId = arrivalAirport.timeZone
        ).mapLeft { FlightInfoInvalidDataError(IllegalStateException(it.head.message)) }.bind()

        ScheduledFlightInfo(
            id = identity,
            version = AggregateVersion(0),
            departurePoint = departurePoint,
            arrivalPoint = arrivalPoint,
            scheduledDepartureTime = kotlin.time.Instant.parse(scheduledDepartureTime.toInstant().toString()),
            scheduledArrivalTime = kotlin.time.Instant.parse(scheduledArrivalTime.toInstant().toString())
        ).mapLeft { FlightInfoInvalidDataError(IllegalStateException(it.head.message)) }.bind()
    }
}
