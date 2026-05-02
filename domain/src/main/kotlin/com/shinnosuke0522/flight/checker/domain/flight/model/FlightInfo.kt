package com.shinnosuke0522.flight.checker.domain.flight.model

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateRoot
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.base.model.MonitoringStatus
import com.shinnosuke0522.flight.checker.domain.shared.value.FlightIdentity
import java.time.Instant
import java.time.LocalDate

sealed interface FlightInfo : AggregateRoot<FlightIdentity> {
    val flightIdentity: FlightIdentity
    val departurePoint: FlightPoint
    val arrivalPoint: FlightPoint
    val scheduledDepartureTime: Instant
    val scheduledArrivalTime: Instant
    val monitoringStatus: MonitoringStatus

    override val id: FlightIdentity get() = flightIdentity
}

@ConsistentCopyVisibility
data class OnScheduleFlightInfo private constructor(
    override val flightIdentity: FlightIdentity,
    override val departurePoint: FlightPoint,
    override val arrivalPoint: FlightPoint,
    override val scheduledDepartureTime: Instant,
    override val scheduledArrivalTime: Instant,
    override val monitoringStatus: MonitoringStatus = MonitoringStatus.IDLE,
    override val version: AggregateVersion = AggregateVersion(),
) : FlightInfo {
    companion object {
        operator fun invoke(
            flightIdentity: FlightIdentity,
            departurePoint: FlightPoint,
            arrivalPoint: FlightPoint,
            scheduledDepartureTime: Instant,
            scheduledArrivalTime: Instant,
            monitoringStatus: MonitoringStatus = MonitoringStatus.IDLE,
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
                monitoringStatus = monitoringStatus,
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
    override val monitoringStatus: MonitoringStatus = MonitoringStatus.IDLE,
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
            monitoringStatus: MonitoringStatus = MonitoringStatus.IDLE,
            version: AggregateVersion = AggregateVersion(),
        ) = DelayedFlightInfo(
            flightIdentity,
            departurePoint,
            arrivalPoint,
            scheduledDepartureTime,
            scheduledArrivalTime,
            estimatedDepartureTime,
            estimatedArrivalTime,
            monitoringStatus,
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
    override val monitoringStatus: MonitoringStatus = MonitoringStatus.IDLE,
    override val version: AggregateVersion = AggregateVersion(),
) : FlightInfo {
    companion object {
        operator fun invoke(
            flightIdentity: FlightIdentity,
            departurePoint: FlightPoint,
            arrivalPoint: FlightPoint,
            scheduledDepartureTime: Instant,
            scheduledArrivalTime: Instant,
            monitoringStatus: MonitoringStatus = MonitoringStatus.IDLE,
            version: AggregateVersion = AggregateVersion(),
        ) = CanceledFlightInfo(
            flightIdentity,
            departurePoint,
            arrivalPoint,
            scheduledDepartureTime,
            scheduledArrivalTime,
            monitoringStatus,
            version
        )
    }
}

private object FlightInfoFactory {
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
            OnScheduleFlightInfo.Companion(
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
