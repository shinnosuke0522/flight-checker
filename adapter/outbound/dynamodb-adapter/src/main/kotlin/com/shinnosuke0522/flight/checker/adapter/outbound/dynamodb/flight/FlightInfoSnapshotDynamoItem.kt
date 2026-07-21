package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.flight

import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.flight.model.ArrivedFlightInfo
import com.shinnosuke0522.flight.checker.domain.flight.model.CanceledFlightInfo
import com.shinnosuke0522.flight.checker.domain.flight.model.DelayedFlightInfo
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightInfo
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightPoint
import com.shinnosuke0522.flight.checker.domain.flight.model.MonitoringStatus
import com.shinnosuke0522.flight.checker.domain.flight.model.ScheduledFlightInfo
import com.shinnosuke0522.flight.checker.domain.flight.model.UncertainFlightInfo
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import java.time.Instant

@Suppress("DataClassShouldBeImmutable")
@DynamoDbBean
data class FlightInfoSnapshotDynamoItem(
    @get:DynamoDbPartitionKey
    var flightIdentity: String = "",
    var version: Long = 0L,
    var type: String = "",
    var monitoringStatus: String = "",
    var departureCountryCode: String = "",
    var departureAirportCode: String = "",
    var departureZoneId: String = "",
    var arrivalCountryCode: String = "",
    var arrivalAirportCode: String = "",
    var arrivalZoneId: String = "",
    var scheduledDepartureTime: String = "",
    var scheduledArrivalTime: String = "",
    var estimatedDepartureTime: String? = null,
    var estimatedArrivalTime: String? = null,
    var reason: String? = null
) {
    fun toDomain(): FlightInfo {
        val identity = FlightIdentity.fromString(flightIdentity)
            .fold({ error(it.toString()) }, { it })

        val domainVersion = AggregateVersion(version)
        val status = MonitoringStatus.valueOf(monitoringStatus)

        val departurePoint = FlightPoint.create(
            countryCode = departureCountryCode,
            airportCode = departureAirportCode,
            zoneId = departureZoneId
        ).fold({ error(it.toString()) }, { it })

        val arrivalPoint = FlightPoint.create(
            countryCode = arrivalCountryCode,
            airportCode = arrivalAirportCode,
            zoneId = arrivalZoneId
        ).fold({ error(it.toString()) }, { it })

        val scheduledDepTime = Instant.parse(scheduledDepartureTime)
        val scheduledArrTime = Instant.parse(scheduledArrivalTime)

        return when (type) {
            "Scheduled" -> ScheduledFlightInfo(
                id = identity,
                version = domainVersion,
                departurePoint = departurePoint,
                arrivalPoint = arrivalPoint,
                scheduledDepartureTime = scheduledDepTime,
                scheduledArrivalTime = scheduledArrTime,
                monitoringStatus = status
            ).fold({ error(it.toString()) }, { it })
            "Delayed" -> DelayedFlightInfo(
                flightIdentity = identity,
                version = domainVersion,
                departurePoint = departurePoint,
                arrivalPoint = arrivalPoint,
                scheduledDepartureTime = scheduledDepTime,
                scheduledArrivalTime = scheduledArrTime,
                estimatedDepartureTime = estimatedDepartureTime?.let { Instant.parse(it) },
                estimatedArrivalTime = estimatedArrivalTime?.let { Instant.parse(it) },
                monitoringStatus = status
            )
            "Arrived" -> ArrivedFlightInfo(
                flightIdentity = identity,
                version = domainVersion,
                departurePoint = departurePoint,
                arrivalPoint = arrivalPoint,
                scheduledDepartureTime = scheduledDepTime,
                scheduledArrivalTime = scheduledArrTime,
                monitoringStatus = status
            )
            "Canceled" -> CanceledFlightInfo(
                id = identity,
                version = domainVersion,
                departurePoint = departurePoint,
                arrivalPoint = arrivalPoint,
                scheduledDepartureTime = scheduledDepTime,
                scheduledArrivalTime = scheduledArrTime,
                monitoringStatus = status
            )
            "Uncertain" -> UncertainFlightInfo(
                id = identity,
                version = domainVersion,
                departurePoint = departurePoint,
                arrivalPoint = arrivalPoint,
                scheduledDepartureTime = scheduledDepTime,
                scheduledArrivalTime = scheduledArrTime,
                reason = reason ?: error("UncertainFlightInfo must have a reason"),
                monitoringStatus = status
            )
            else -> error("Unknown flight info type: $type")
        }
    }

    companion object {
        fun fromDomain(flight: FlightInfo): FlightInfoSnapshotDynamoItem {
            return FlightInfoSnapshotDynamoItem(
                flightIdentity = flight.id.asString(),
                version = flight.version.value,
                type = when (flight) {
                    is ScheduledFlightInfo -> "Scheduled"
                    is DelayedFlightInfo -> "Delayed"
                    is ArrivedFlightInfo -> "Arrived"
                    is CanceledFlightInfo -> "Canceled"
                    is UncertainFlightInfo -> "Uncertain"
                },
                monitoringStatus = flight.monitoringStatus.name,
                departureCountryCode = flight.departurePoint.countryCode.value,
                departureAirportCode = flight.departurePoint.airportCode.value,
                departureZoneId = flight.departurePoint.zoneId.id,
                arrivalCountryCode = flight.arrivalPoint.countryCode.value,
                arrivalAirportCode = flight.arrivalPoint.airportCode.value,
                arrivalZoneId = flight.arrivalPoint.zoneId.id,
                scheduledDepartureTime = flight.scheduledDepartureTime.toString(),
                scheduledArrivalTime = flight.scheduledArrivalTime.toString(),
                estimatedDepartureTime = (flight as? DelayedFlightInfo)?.estimatedDepartureTime?.toString(),
                estimatedArrivalTime = (flight as? DelayedFlightInfo)?.estimatedArrivalTime?.toString(),
                reason = (flight as? UncertainFlightInfo)?.reason
            )
        }
    }
}
