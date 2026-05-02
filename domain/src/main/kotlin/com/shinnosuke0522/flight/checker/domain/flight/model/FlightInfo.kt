package com.shinnosuke0522.flight.checker.domain.flight.model

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateRoot
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.shared.value.FlightIdentity
import java.time.Instant
import java.time.LocalDate

sealed interface FlightInfo : AggregateRoot<FlightIdentity> {
    val flightIdentity: FlightIdentity
    val departurePoint: FlightPoint
    val arrivalPoint: FlightPoint
    val scheduledDepartureTime: Instant
    val scheduledArrivalTime: Instant

    override val id: FlightIdentity get() = flightIdentity
}

@ConsistentCopyVisibility
data class OnScheduleFlightInfo private constructor(
    override val flightIdentity: FlightIdentity,
    override val departurePoint: FlightPoint,
    override val arrivalPoint: FlightPoint,
    override val scheduledDepartureTime: Instant,
    override val scheduledArrivalTime: Instant,
    override val version: AggregateVersion = AggregateVersion(),
) : FlightInfo {
    companion object {
        operator fun invoke(
            flightIdentity: FlightIdentity,
            departurePoint: FlightPoint,
            arrivalPoint: FlightPoint,
            scheduledDepartureTime: Instant,
            scheduledArrivalTime: Instant,
            version: AggregateVersion = AggregateVersion(),
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
                scheduledArrivalTime = scheduledArrivalTime,
                version = version
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
    override val version: AggregateVersion = AggregateVersion(),
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
            version: AggregateVersion = AggregateVersion(),
        ) = DelayedFlightInfo(
            flightIdentity,
            departurePoint,
            arrivalPoint,
            scheduledDepartureTime,
            scheduledArrivalTime,
            estimatedDepartureTime,
            estimatedArrivalTime,
            version
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
    override val version: AggregateVersion = AggregateVersion(),
) : FlightInfo {
    companion object {
        operator fun invoke(
            flightIdentity: FlightIdentity,
            departurePoint: FlightPoint,
            arrivalPoint: FlightPoint,
            scheduledDepartureTime: Instant,
            scheduledArrivalTime: Instant,
            version: AggregateVersion = AggregateVersion(),
        ) = CanceledFlightInfo(
            flightIdentity,
            departurePoint,
            arrivalPoint,
            scheduledDepartureTime,
            scheduledArrivalTime,
            version
        )
    }
}
