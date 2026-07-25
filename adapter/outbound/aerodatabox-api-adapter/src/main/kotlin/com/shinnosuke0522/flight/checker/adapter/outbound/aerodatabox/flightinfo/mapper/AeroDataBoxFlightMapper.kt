package com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.flightinfo.mapper

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.model.FlightContract
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.flight.error.FlightInfoInvalidDataError
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightInfo
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightPoint
import com.shinnosuke0522.flight.checker.domain.flight.model.ScheduledFlightInfo
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import java.time.Instant

object AeroDataBoxFlightMapper {
    fun toDomain(
        flight: FlightContract,
        identity: FlightIdentity
    ): Either<FlightInfoInvalidDataError, FlightInfo> = either {
        val departureAirport = flight.departure.airport
        val arrivalAirport = flight.arrival.airport

        val scheduledDepartureTimeStr = flight.departure.scheduledTime?.utc
        val scheduledArrivalTimeStr = flight.arrival.scheduledTime?.utc

        ensureNotNull(scheduledDepartureTimeStr) {
            FlightInfoInvalidDataError(IllegalArgumentException("Scheduled departure time is missing"))
        }
        ensureNotNull(scheduledArrivalTimeStr) {
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
            scheduledDepartureTime = Instant.parse(scheduledDepartureTimeStr.toString()),
            scheduledArrivalTime = Instant.parse(scheduledArrivalTimeStr.toString())
        ).mapLeft { FlightInfoInvalidDataError(IllegalStateException(it.head.message)) }.bind()
    }
}
