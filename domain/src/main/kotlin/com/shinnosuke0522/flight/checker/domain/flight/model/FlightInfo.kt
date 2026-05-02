package com.shinnosuke0522.flight.checker.domain.flight.model

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.context.bind
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import com.shinnosuke0522.flight.checker.domain.base.error.Error
import com.shinnosuke0522.flight.checker.domain.shared.value.FlightIdentity
import java.time.Instant
import java.time.LocalDate

sealed interface FlightInfo {
    val flightIdentity: FlightIdentity
    val departurePoint: FlightPoint
    val arrivalPoint: FlightPoint
    val scheduledDepartureTime: Instant
    val scheduledArrivalTime: Instant
}

@ConsistentCopyVisibility
data class OnScheduleFlightInfo private constructor(
    override val flightIdentity: FlightIdentity,
    override val departurePoint: FlightPoint,
    override val arrivalPoint: FlightPoint,
    override val scheduledDepartureTime: Instant,
    override val scheduledArrivalTime: Instant,
) : FlightInfo {
    companion object {
        operator fun invoke(
            flightIdentity: FlightIdentity,
            departurePoint: FlightPoint,
            arrivalPoint: FlightPoint,
            scheduledDepartureTime: Instant,
            scheduledArrivalTime: Instant
        ): Either<NonEmptyList<FlightInfoValidationError>, OnScheduleFlightInfo> = either {
            ensure(scheduledDepartureTime != scheduledArrivalTime) {
                nonEmptyListOf(SameFlightPointError)
            }
            ensure(scheduledDepartureTime.isBefore(scheduledArrivalTime)) {
                nonEmptyListOf(ScheduledArrivalTimeBeforeDepartureTimeError)
            }
            OnScheduleFlightInfo(
                flightIdentity = flightIdentity,
                departurePoint = departurePoint,
                arrivalPoint = arrivalPoint,
                scheduledDepartureTime = scheduledDepartureTime,
                scheduledArrivalTime = scheduledArrivalTime
            )
        }

        fun create(
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
        ): Either<NonEmptyList<FlightInfoValidationError>, OnScheduleFlightInfo> =
            FlightInfoFactory.createOnSchedule(
                flightCode = flightCode,
                departureDate = departureDate,
                departureCountryCode = departureCountryCode,
                departureAirportCode = departureAirportCode,
                departureZoneId = departureZoneId,
                arrivalCountryCode = arrivalCountryCode,
                arrivalAirportCode = arrivalAirportCode,
                arrivalZoneId = arrivalZoneId,
                scheduledDepartureTime = scheduledDepartureTime,
                scheduledArrivalTime = scheduledArrivalTime
            )
    }
}

@ConsistentCopyVisibility
data class DelayedFlightInfo private constructor(
    override val flightIdentity: FlightIdentity,
    override val departurePoint: FlightPoint,
    override val arrivalPoint: FlightPoint,
    override val scheduledDepartureTime: Instant,
    override val scheduledArrivalTime: Instant,
    val estimatedDepartureTime: Instant?,
    val estimatedArrivalTime: Instant?,
) : FlightInfo {
    companion object {
        operator fun invoke(
            flightIdentity: FlightIdentity,
            departurePoint: FlightPoint,
            arrivalPoint: FlightPoint,
            scheduledDepartureTime: Instant,
            scheduledArrivalTime: Instant,
            estimatedDepartureTime: Instant? = null,
            estimatedArrivalTime: Instant? = null,
        ) = DelayedFlightInfo(
            flightIdentity,
            departurePoint,
            arrivalPoint,
            scheduledDepartureTime,
            scheduledArrivalTime,
            estimatedDepartureTime,
            estimatedArrivalTime
        )
    }
}

@ConsistentCopyVisibility
data class CanceledFlightInfo private constructor(
    override val flightIdentity: FlightIdentity,
    override val departurePoint: FlightPoint,
    override val arrivalPoint: FlightPoint,
    override val scheduledDepartureTime: Instant,
    override val scheduledArrivalTime: Instant,
) : FlightInfo {
    companion object {
        operator fun invoke(
            flightIdentity: FlightIdentity,
            departurePoint: FlightPoint,
            arrivalPoint: FlightPoint,
            scheduledDepartureTime: Instant,
            scheduledArrivalTime: Instant,
        ) = CanceledFlightInfo(
            flightIdentity,
            departurePoint,
            arrivalPoint,
            scheduledDepartureTime,
            scheduledArrivalTime
        )
    }
}
