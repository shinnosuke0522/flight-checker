package com.shinnosuke0522.flight.checker.domain.flight.model

import arrow.core.nonEmptyListOf
import com.shinnosuke0522.flight.checker.domain.base.event.CorrelationId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightDelayed
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightInfoRegistered
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightMonitoringActivated
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightStatusUncertain
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Instant
import java.time.LocalDate

class FlightInfoTest : DescribeSpec({
    describe("Test: replay") {
        context("Given: 新しくフライトが登録された場合") {
            val flightIdentity = FlightIdentity.create("JL123", LocalDate.of(2026, 5, 1)).shouldBeRight()
            val departurePoint = FlightPoint.create("JP", "HND", "Asia/Tokyo").shouldBeRight()
            val arrivalPoint = FlightPoint.create("US", "JFK", "America/New_York").shouldBeRight()
            val departureTime = Instant.parse("2026-05-01T10:00:00Z")
            val arrivalTime = Instant.parse("2026-05-01T23:00:00Z")

            val event = FlightInfoRegistered(
                id = DomainEventId.generate(),
                aggregateId = flightIdentity,
                sequenceNumber = 0,
                meta = DomainEventMeta(Instant.now(), CorrelationId.generate()),
                departurePoint = departurePoint,
                arrivalPoint = arrivalPoint,
                scheduledDepartureTime = departureTime,
                scheduledArrivalTime = arrivalTime
            )

            context("When: 登録イベントを適用して状態を復元すると") {
                val flightInfo = FlightInfo.replay(nonEmptyListOf(event))

                it("Then: 予定通りのフライト情報 (ScheduledFlightInfo) として再構築されること") {
                    flightInfo.shouldBeInstanceOf<ScheduledFlightInfo>()
                    flightInfo.id shouldBe flightIdentity
                    flightInfo.departurePoint shouldBe departurePoint
                    flightInfo.arrivalPoint shouldBe arrivalPoint
                    flightInfo.scheduledDepartureTime shouldBe departureTime
                    flightInfo.scheduledArrivalTime shouldBe arrivalTime
                    flightInfo.monitoringStatus shouldBe MonitoringStatus.IDLE
                    flightInfo.version shouldBe AggregateVersion(0)
                }
            }
        }

        context("Given: 登録済みのフライトに対して遅延が発生した場合") {
            val (registeredEvent, initialInfo) = createSampleRegisteredEventAndInfo()
            val estimatedDepartureTime = initialInfo.scheduledDepartureTime.plusSeconds(3600)

            val delayedEvent = FlightDelayed(
                id = DomainEventId.generate(),
                aggregateId = initialInfo.id,
                sequenceNumber = 1,
                meta = DomainEventMeta(Instant.now(), CorrelationId.generate()),
                estimatedDepartureTime = estimatedDepartureTime,
                estimatedArrivalTime = null
            )

            context("When: 遅延イベントを含めて状態を復元すると") {
                val updatedInfo = FlightInfo.replay(nonEmptyListOf(registeredEvent, delayedEvent))

                it("Then: 遅延フライト情報 (DelayedFlightInfo) に遷移し、見積時刻が反映されること") {
                    updatedInfo.shouldBeInstanceOf<DelayedFlightInfo>()
                    updatedInfo.estimatedDepartureTime shouldBe estimatedDepartureTime
                    updatedInfo.monitoringStatus shouldBe MonitoringStatus.ACTIVATED
                    updatedInfo.version shouldBe AggregateVersion(1)
                }
            }
        }

        context("Given: フライトの動静が不明確になった場合") {
            val (registeredEvent, initialInfo) = createSampleRegisteredEventAndInfo()
            val reason = "No updates from API for 30 minutes"
            val uncertainEvent = FlightStatusUncertain(
                id = DomainEventId.generate(),
                aggregateId = initialInfo.id,
                sequenceNumber = 1,
                meta = DomainEventMeta(Instant.now(), CorrelationId.generate()),
                reason = reason
            )

            context("When: 不確定イベントを含めて状態を復元すると") {
                val updatedInfo = FlightInfo.replay(nonEmptyListOf(registeredEvent, uncertainEvent))

                it("Then: 不確定フライト情報 (UncertainFlightInfo) に遷移し、理由が記録されること") {
                    updatedInfo.shouldBeInstanceOf<UncertainFlightInfo>()
                    updatedInfo.reason shouldBe reason
                    updatedInfo.monitoringStatus shouldBe MonitoringStatus.ACTIVATED
                    updatedInfo.version shouldBe AggregateVersion(1)
                }
            }
        }

        context("Given: フライトの監視が有効化された場合") {
            val (registeredEvent, initialInfo) = createSampleRegisteredEventAndInfo()
            val activatedEvent = FlightMonitoringActivated(
                id = DomainEventId.generate(),
                aggregateId = initialInfo.id,
                sequenceNumber = 1,
                meta = DomainEventMeta(Instant.now(), CorrelationId.generate())
            )

            context("When: 有効化イベントを含めて状態を復元すると") {
                val updatedInfo = FlightInfo.replay(nonEmptyListOf(registeredEvent, activatedEvent))

                it("Then: 監視ステータスが ACTIVATED に更新されること") {
                    updatedInfo.monitoringStatus shouldBe MonitoringStatus.ACTIVATED
                    updatedInfo.version shouldBe AggregateVersion(1)
                }
            }
        }
    }
})

private fun createSampleRegisteredEventAndInfo(): Pair<FlightInfoRegistered, FlightInfo> {
    val id = FlightIdentity.create("JL123", LocalDate.of(2026, 5, 1)).shouldBeRight()
    val departurePoint = FlightPoint.create("JP", "HND", "Asia/Tokyo").shouldBeRight()
    val arrivalPoint = FlightPoint.create("US", "JFK", "America/New_York").shouldBeRight()
    val event = FlightInfoRegistered(
        id = DomainEventId.generate(),
        aggregateId = id,
        sequenceNumber = 0,
        meta = DomainEventMeta(Instant.now(), CorrelationId.generate()),
        departurePoint = departurePoint,
        arrivalPoint = arrivalPoint,
        scheduledDepartureTime = Instant.parse("2026-05-01T10:00:00Z"),
        scheduledArrivalTime = Instant.parse("2026-05-01T23:00:00Z")
    )
    return event to FlightInfo.replay(nonEmptyListOf(event))
}
