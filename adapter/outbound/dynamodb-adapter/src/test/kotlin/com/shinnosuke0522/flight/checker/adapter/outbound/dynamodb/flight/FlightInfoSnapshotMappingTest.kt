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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.engine.names.WithDataTestName
import io.kotest.matchers.shouldBe
import java.time.Instant

class FlightInfoSnapshotMappingTest : FunSpec({
    context("FlightInfoSnapshotDynamoItem のマッピング検証") {
        context("正常系: Domain -> Item -> Domain の相互変換ができること") {
            withData(
                SnapshotMappingTestCase(
                    name = "ScheduledFlightInfo",
                    domain = ScheduledFlightInfo(
                        id = aggregateId,
                        version = version,
                        departurePoint = departurePoint,
                        arrivalPoint = arrivalPoint,
                        scheduledDepartureTime = scheduledDepartureTime,
                        scheduledArrivalTime = scheduledArrivalTime,
                        monitoringStatus = MonitoringStatus.ACTIVATED
                    ).fold({ error(it.toString()) }, { it })
                ),
                SnapshotMappingTestCase(
                    name = "DelayedFlightInfo",
                    domain = DelayedFlightInfo(
                        flightIdentity = aggregateId,
                        version = version,
                        departurePoint = departurePoint,
                        arrivalPoint = arrivalPoint,
                        scheduledDepartureTime = scheduledDepartureTime,
                        scheduledArrivalTime = scheduledArrivalTime,
                        estimatedDepartureTime = Instant.parse("2026-05-01T11:00:00Z"),
                        estimatedArrivalTime = Instant.parse("2026-05-01T13:00:00Z"),
                        monitoringStatus = MonitoringStatus.ACTIVATED
                    )
                ),
                SnapshotMappingTestCase(
                    name = "ArrivedFlightInfo",
                    domain = ArrivedFlightInfo(
                        flightIdentity = aggregateId,
                        version = version,
                        departurePoint = departurePoint,
                        arrivalPoint = arrivalPoint,
                        scheduledDepartureTime = scheduledDepartureTime,
                        scheduledArrivalTime = scheduledArrivalTime,
                        monitoringStatus = MonitoringStatus.COMPLETED
                    )
                ),
                SnapshotMappingTestCase(
                    name = "CanceledFlightInfo",
                    domain = CanceledFlightInfo(
                        id = aggregateId,
                        version = version,
                        departurePoint = departurePoint,
                        arrivalPoint = arrivalPoint,
                        scheduledDepartureTime = scheduledDepartureTime,
                        scheduledArrivalTime = scheduledArrivalTime,
                        monitoringStatus = MonitoringStatus.COMPLETED
                    )
                ),
                SnapshotMappingTestCase(
                    name = "UncertainFlightInfo",
                    domain = UncertainFlightInfo(
                        id = aggregateId,
                        version = version,
                        departurePoint = departurePoint,
                        arrivalPoint = arrivalPoint,
                        scheduledDepartureTime = scheduledDepartureTime,
                        scheduledArrivalTime = scheduledArrivalTime,
                        reason = "Weather issue",
                        monitoringStatus = MonitoringStatus.FAILED
                    )
                )
            ) { testCase ->
                val item = FlightInfoSnapshotDynamoItem.fromDomain(testCase.domain)
                val restored = item.toDomain()
                restored shouldBe testCase.domain
            }
        }

        context("異常系: 不正な Item を Domain に復元しようとした場合") {
            withData(
                SnapshotErrorMappingTestCase(
                    name = "未知のフライトタイプ",
                    item = FlightInfoSnapshotDynamoItem(
                        flightIdentity = aggregateId.asString(),
                        version = version.value,
                        type = "UnknownType",
                        monitoringStatus = "ACTIVATED",
                        departureCountryCode = "JP",
                        departureAirportCode = "HND",
                        departureZoneId = "Asia/Tokyo",
                        arrivalCountryCode = "US",
                        arrivalAirportCode = "JFK",
                        arrivalZoneId = "America/New_York",
                        scheduledDepartureTime = scheduledDepartureTime.toString(),
                        scheduledArrivalTime = scheduledArrivalTime.toString()
                    )
                ),
                SnapshotErrorMappingTestCase(
                    name = "不正な FlightIdentity",
                    item = FlightInfoSnapshotDynamoItem(
                        flightIdentity = "invalid-format", // Invalid
                        version = version.value,
                        type = "Scheduled",
                        monitoringStatus = "ACTIVATED",
                        departureCountryCode = "JP",
                        departureAirportCode = "HND",
                        departureZoneId = "Asia/Tokyo",
                        arrivalCountryCode = "US",
                        arrivalAirportCode = "JFK",
                        arrivalZoneId = "America/New_York",
                        scheduledDepartureTime = scheduledDepartureTime.toString(),
                        scheduledArrivalTime = scheduledArrivalTime.toString()
                    )
                ),
                SnapshotErrorMappingTestCase(
                    name = "UncertainFlightInfoでreasonがnull",
                    item = FlightInfoSnapshotDynamoItem(
                        flightIdentity = aggregateId.asString(),
                        version = version.value,
                        type = "Uncertain",
                        monitoringStatus = "FAILED",
                        departureCountryCode = "JP",
                        departureAirportCode = "HND",
                        departureZoneId = "Asia/Tokyo",
                        arrivalCountryCode = "US",
                        arrivalAirportCode = "JFK",
                        arrivalZoneId = "America/New_York",
                        scheduledDepartureTime = scheduledDepartureTime.toString(),
                        scheduledArrivalTime = scheduledArrivalTime.toString(),
                        reason = null // Invalid for Uncertain
                    )
                )
            ) { testCase ->
                shouldThrow<Exception> {
                    testCase.item.toDomain()
                }
            }
        }
    }
}) {
    companion object {
        val aggregateId = FlightIdentity.create("JL123", java.time.LocalDate.of(2026, 5, 1)).fold({
            error(it.toString())
        }, { it })
        val version = AggregateVersion(1L)
        val departurePoint = FlightPoint.create("JP", "HND", "Asia/Tokyo").fold({ error(it.toString()) }, { it })
        val arrivalPoint = FlightPoint.create("US", "JFK", "America/New_York").fold({ error(it.toString()) }, { it })
        val scheduledDepartureTime: Instant = Instant.parse("2026-05-01T10:00:00Z")
        val scheduledArrivalTime: Instant = Instant.parse("2026-05-01T12:00:00Z")
    }
}

data class SnapshotMappingTestCase(
    val name: String,
    val domain: FlightInfo
) : WithDataTestName {
    override fun dataTestName() = name
}

data class SnapshotErrorMappingTestCase(
    val name: String,
    val item: FlightInfoSnapshotDynamoItem
) : WithDataTestName {
    override fun dataTestName() = name
}
