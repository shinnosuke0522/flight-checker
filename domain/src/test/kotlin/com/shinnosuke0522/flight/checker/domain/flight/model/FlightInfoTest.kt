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

                it("Then: 正しくプロパティがセットされていること") {
                    flightInfo.id shouldBe flightIdentity
                    flightInfo.version shouldBe AggregateVersion(0)
                    flightInfo.departurePoint shouldBe departurePoint
                    flightInfo.arrivalPoint shouldBe arrivalPoint
                    flightInfo.scheduledDepartureTime shouldBe departureTime
                    flightInfo.scheduledArrivalTime shouldBe arrivalTime
                    flightInfo.monitoringStatus shouldBe MonitoringStatus.IDLE
                }
            }
        }

        context("Given: 監視が有効化されたフライトの場合") {
            val base = givenFlightInfo()
            val event = FlightMonitoringActivated(
                id = DomainEventId.generate(),
                aggregateId = base.id,
                sequenceNumber = 1,
                meta = DomainEventMeta(Instant.now(), CorrelationId.generate())
            )

            context("When: 活性化イベントを適用すると") {
                val flightInfo = base.apply(event)

                it("Then: 監視ステータスが ACTIVATED になっていること") {
                    flightInfo.monitoringStatus shouldBe MonitoringStatus.ACTIVATED
                    flightInfo.version shouldBe AggregateVersion(1)
                }
            }
        }

        context("Given: 遅延が発生したフライトの場合") {
            val base = givenFlightInfo()
            val newDepartureTime = Instant.parse("2026-05-01T11:00:00Z")
            val newArrivalTime = Instant.parse("2026-05-02T00:00:00Z")
            val event = FlightDelayed(
                id = DomainEventId.generate(),
                aggregateId = base.id,
                sequenceNumber = 1,
                meta = DomainEventMeta(Instant.now(), CorrelationId.generate()),
                estimatedDepartureTime = newDepartureTime,
                estimatedArrivalTime = newArrivalTime
            )

            context("When: 遅延イベントを適用すると") {
                val flightInfo = base.apply(event)

                it("Then: 推定時刻が更新されていること") {
                    flightInfo.scheduledDepartureTime shouldBe newDepartureTime
                    flightInfo.scheduledArrivalTime shouldBe newArrivalTime
                }
            }
        }

        context("Given: 状況が不確実になったフライトの場合") {
            val base = givenFlightInfo()
            val event = FlightStatusUncertain(
                id = DomainEventId.generate(),
                aggregateId = base.id,
                sequenceNumber = 1,
                meta = DomainEventMeta(Instant.now(), CorrelationId.generate()),
                reason = "Weather condition"
            )

            context("When: 不確実イベントを適用すると") {
                val flightInfo = base.apply(event)

                it("Then: ステータスは変わるが時刻等はそのまま維持されること") {
                    flightInfo.version shouldBe AggregateVersion(1)
                    flightInfo.scheduledDepartureTime shouldBe base.scheduledDepartureTime
                }
            }
        }
    }
})

private fun givenFlightInfo(): FlightInfo {
    val id = FlightIdentity.create("JL123", LocalDate.of(2026, 5, 1)).shouldBeRight()
    val departurePoint = FlightPoint.create("JP", "HND", "Asia/Tokyo").shouldBeRight()
    val arrivalPoint = FlightPoint.create("US", "JFK", "America/New_York").shouldBeRight()
    val departureTime = Instant.parse("2026-05-01T10:00:00Z")
    val arrivalTime = Instant.parse("2026-05-01T23:00:00Z")

    val event = FlightInfoRegistered(
        id = DomainEventId.generate(),
        aggregateId = id,
        sequenceNumber = 0,
        meta = DomainEventMeta(Instant.now(), CorrelationId.generate()),
        departurePoint = departurePoint,
        arrivalPoint = arrivalPoint,
        scheduledDepartureTime = departureTime,
        scheduledArrivalTime = arrivalTime
    )
    return FlightInfo.replay(nonEmptyListOf(event))
}
