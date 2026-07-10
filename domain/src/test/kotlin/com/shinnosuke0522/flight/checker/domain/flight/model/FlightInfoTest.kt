package com.shinnosuke0522.flight.checker.domain.flight.model

import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import com.shinnosuke0522.flight.checker.domain.base.event.CorrelationId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightArrived
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightCanceled
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightDelayed
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightInfoEvent
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightInfoRegistered
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightMonitoringCompleted
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightMonitoringFailed
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightStatusUncertain
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.engine.names.WithDataTestName
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.LocalDate

data class FlightInfoApplyTestCase(
    val name: String,
    val initialFlightInfo: FlightInfo,
    val event: FlightInfoEvent,
    val expectedFlightInfo: FlightInfo
) : WithDataTestName {
    override fun dataTestName() = name
}

data class FlightInfoReplayTestCase(
    val name: String,
    val additionalEvents: List<FlightInfoEvent>,
    val expectedFlightInfo: FlightInfo
) : WithDataTestName {
    override fun dataTestName() = name
}

class FlightInfoTest : FunSpec({
    context("イベントの適用 (Apply) による状態遷移が正しく行われること") {
        withData(
            FlightInfoApplyTestCase(
                name = "ScheduledFlightInfo に遅延イベントを適用すると、DelayedFlightInfo に遷移する",
                initialFlightInfo = initialFlightInfo,
                event = FlightDelayed(
                    id = DomainEventId.generate(),
                    aggregateId = flightIdentity,
                    sequenceNumber = 1,
                    meta = DomainEventMeta(Instant.now(), CorrelationId.generate()),
                    estimatedDepartureTime = newDepartureTime,
                    estimatedArrivalTime = newArrivalTime
                ),
                expectedFlightInfo = DelayedFlightInfo(
                    flightIdentity = flightIdentity,
                    version = AggregateVersion(1),
                    departurePoint = departurePoint,
                    arrivalPoint = arrivalPoint,
                    scheduledDepartureTime = departureTime,
                    scheduledArrivalTime = arrivalTime,
                    estimatedDepartureTime = newDepartureTime,
                    estimatedArrivalTime = newArrivalTime,
                    monitoringStatus = MonitoringStatus.ACTIVATED
                )
            ),
            FlightInfoApplyTestCase(
                name = "ScheduledFlightInfo に欠航イベントを適用すると、CanceledFlightInfo に遷移する",
                initialFlightInfo = initialFlightInfo,
                event = FlightCanceled(
                    id = DomainEventId.generate(),
                    aggregateId = flightIdentity,
                    sequenceNumber = 1,
                    meta = DomainEventMeta(Instant.now(), CorrelationId.generate())
                ),
                expectedFlightInfo = CanceledFlightInfo(
                    id = flightIdentity,
                    version = AggregateVersion(1),
                    departurePoint = departurePoint,
                    arrivalPoint = arrivalPoint,
                    scheduledDepartureTime = departureTime,
                    scheduledArrivalTime = arrivalTime,
                    monitoringStatus = MonitoringStatus.COMPLETED
                )
            )
        ) { testCase ->
            val result = testCase.initialFlightInfo.apply(testCase.event)
            result shouldBe testCase.expectedFlightInfo
        }
    }

    test("CanceledFlightInfo (終端状態) にイベントを適用しようとすると、IllegalStateException がスローされること") {
        val canceledInfo = CanceledFlightInfo(
            id = flightIdentity,
            version = AggregateVersion(1),
            departurePoint = departurePoint,
            arrivalPoint = arrivalPoint,
            scheduledDepartureTime = departureTime,
            scheduledArrivalTime = arrivalTime,
            monitoringStatus = MonitoringStatus.COMPLETED
        )
        val event = FlightDelayed(
            id = DomainEventId.generate(),
            aggregateId = flightIdentity,
            sequenceNumber = 2,
            meta = DomainEventMeta(Instant.now(), CorrelationId.generate()),
            estimatedDepartureTime = newDepartureTime,
            estimatedArrivalTime = newArrivalTime
        )

        shouldThrow<IllegalStateException> {
            canceledInfo.apply(event)
        }
    }

    context("イベント履歴から各状態を復元 (Replay) できること") {
        withData(
            FlightInfoReplayTestCase(
                name = "ScheduledFlightInfo に復元できること",
                additionalEvents = emptyList(),
                expectedFlightInfo = ScheduledFlightInfo(
                    id = flightIdentity,
                    version = AggregateVersion(0),
                    departurePoint = departurePoint,
                    arrivalPoint = arrivalPoint,
                    scheduledDepartureTime = departureTime,
                    scheduledArrivalTime = arrivalTime,
                    monitoringStatus = MonitoringStatus.IDLE
                ).getOrElse { error(it) }
            ),
            FlightInfoReplayTestCase(
                name = "DelayedFlightInfo に復元できること",
                additionalEvents = listOf(
                    FlightDelayed(
                        id = DomainEventId.generate(),
                        aggregateId = flightIdentity,
                        sequenceNumber = 1,
                        meta = DomainEventMeta(Instant.now(), CorrelationId.generate()),
                        estimatedDepartureTime = newDepartureTime,
                        estimatedArrivalTime = newArrivalTime
                    )
                ),
                expectedFlightInfo = DelayedFlightInfo(
                    flightIdentity = flightIdentity,
                    version = AggregateVersion(1),
                    departurePoint = departurePoint,
                    arrivalPoint = arrivalPoint,
                    scheduledDepartureTime = departureTime,
                    scheduledArrivalTime = arrivalTime,
                    estimatedDepartureTime = newDepartureTime,
                    estimatedArrivalTime = newArrivalTime,
                    monitoringStatus = MonitoringStatus.ACTIVATED
                )
            ),
            FlightInfoReplayTestCase(
                name = "UncertainFlightInfo に復元できること",
                additionalEvents = listOf(
                    FlightStatusUncertain(
                        id = DomainEventId.generate(),
                        aggregateId = flightIdentity,
                        sequenceNumber = 1,
                        meta = DomainEventMeta(Instant.now(), CorrelationId.generate()),
                        reason = "Weather condition"
                    )
                ),
                expectedFlightInfo = UncertainFlightInfo(
                    id = flightIdentity,
                    version = AggregateVersion(1),
                    departurePoint = departurePoint,
                    arrivalPoint = arrivalPoint,
                    scheduledDepartureTime = departureTime,
                    scheduledArrivalTime = arrivalTime,
                    reason = "Weather condition",
                    monitoringStatus = MonitoringStatus.ACTIVATED
                )
            ),
            FlightInfoReplayTestCase(
                name = "CanceledFlightInfo に復元できること",
                additionalEvents = listOf(
                    FlightCanceled(
                        id = DomainEventId.generate(),
                        aggregateId = flightIdentity,
                        sequenceNumber = 1,
                        meta = DomainEventMeta(Instant.now(), CorrelationId.generate())
                    )
                ),
                expectedFlightInfo = CanceledFlightInfo(
                    id = flightIdentity,
                    version = AggregateVersion(1),
                    departurePoint = departurePoint,
                    arrivalPoint = arrivalPoint,
                    scheduledDepartureTime = departureTime,
                    scheduledArrivalTime = arrivalTime,
                    monitoringStatus = MonitoringStatus.COMPLETED
                )
            ),
            FlightInfoReplayTestCase(
                name = "ArrivedFlightInfo に復元できること",
                additionalEvents = listOf(
                    FlightArrived(
                        id = DomainEventId.generate(),
                        aggregateId = flightIdentity,
                        sequenceNumber = 1,
                        meta = DomainEventMeta(Instant.now(), CorrelationId.generate())
                    )
                ),
                expectedFlightInfo = ArrivedFlightInfo(
                    flightIdentity = flightIdentity,
                    version = AggregateVersion(1),
                    departurePoint = departurePoint,
                    arrivalPoint = arrivalPoint,
                    scheduledDepartureTime = departureTime,
                    scheduledArrivalTime = arrivalTime,
                    monitoringStatus = MonitoringStatus.COMPLETED
                )
            ),
            FlightInfoReplayTestCase(
                name = "ScheduledFlightInfo (監視完了状態) に復元できること",
                additionalEvents = listOf(
                    FlightMonitoringCompleted(
                        id = DomainEventId.generate(),
                        aggregateId = flightIdentity,
                        sequenceNumber = 1,
                        meta = DomainEventMeta(Instant.now(), CorrelationId.generate())
                    )
                ),
                expectedFlightInfo = ScheduledFlightInfo(
                    id = flightIdentity,
                    version = AggregateVersion(1),
                    departurePoint = departurePoint,
                    arrivalPoint = arrivalPoint,
                    scheduledDepartureTime = departureTime,
                    scheduledArrivalTime = arrivalTime,
                    monitoringStatus = MonitoringStatus.COMPLETED
                ).getOrElse { error(it) }
            ),
            FlightInfoReplayTestCase(
                name = "ScheduledFlightInfo (監視失敗状態) に復元できること",
                additionalEvents = listOf(
                    FlightMonitoringFailed(
                        id = DomainEventId.generate(),
                        aggregateId = flightIdentity,
                        sequenceNumber = 1,
                        meta = DomainEventMeta(Instant.now(), CorrelationId.generate()),
                        reason = "Webhook registration failed"
                    )
                ),
                expectedFlightInfo = ScheduledFlightInfo(
                    id = flightIdentity,
                    version = AggregateVersion(1),
                    departurePoint = departurePoint,
                    arrivalPoint = arrivalPoint,
                    scheduledDepartureTime = departureTime,
                    scheduledArrivalTime = arrivalTime,
                    monitoringStatus = MonitoringStatus.FAILED
                ).getOrElse { error(it) }
            )
        ) { testCase ->
            val events = nonEmptyListOf(registeredEvent, *testCase.additionalEvents.toTypedArray())
            val flightInfo = FlightInfo.replay(events)
            flightInfo shouldBe testCase.expectedFlightInfo
        }
    }
}) {

    companion object {
        val flightIdentity = FlightIdentity.create("JL123", LocalDate.of(2026, 5, 1)).getOrElse { error(it) }
        val departurePoint = FlightPoint.create("JP", "HND", "Asia/Tokyo").getOrElse { error(it) }
        val arrivalPoint = FlightPoint.create("US", "JFK", "America/New_York").getOrElse { error(it) }
        val departureTime: Instant = Instant.parse("2026-05-01T10:00:00Z")
        val arrivalTime: Instant = Instant.parse("2026-05-01T23:00:00Z")

        val registeredEvent = FlightInfoRegistered(
            id = DomainEventId.generate(),
            aggregateId = flightIdentity,
            sequenceNumber = 0,
            meta = DomainEventMeta(Instant.now(), CorrelationId.generate()),
            departurePoint = departurePoint,
            arrivalPoint = arrivalPoint,
            scheduledDepartureTime = departureTime,
            scheduledArrivalTime = arrivalTime
        )

        val newDepartureTime: Instant = Instant.parse("2026-05-01T11:00:00Z")
        val newArrivalTime: Instant = Instant.parse("2026-05-02T00:00:00Z")

        val initialFlightInfo = ScheduledFlightInfo(
            id = flightIdentity,
            version = AggregateVersion(0),
            departurePoint = departurePoint,
            arrivalPoint = arrivalPoint,
            scheduledDepartureTime = departureTime,
            scheduledArrivalTime = arrivalTime,
            monitoringStatus = MonitoringStatus.IDLE
        ).getOrElse { error(it) }
    }
}
