package com.shinnosuke0522.flight.checker.domain.flight.model

import arrow.core.nonEmptyListOf
import com.shinnosuke0522.flight.checker.domain.base.event.CorrelationId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightArrived
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightCanceled
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightDelayed
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightInfoRegistered
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightMonitoringActivated
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightMonitoringCompleted
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightMonitoringFailed
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightOnScheduleReturned
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightStatusUncertain
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import java.time.Instant
import java.time.LocalDate

class FlightInfoTest : DescribeSpec({
    describe("Test: replay") {
        val flightIdentity = FlightIdentity.create("JL123", LocalDate.of(2026, 5, 1)).shouldBeRight()
        val departurePoint = FlightPoint.create("JP", "HND", "Asia/Tokyo").shouldBeRight()
        val arrivalPoint = FlightPoint.create("US", "JFK", "America/New_York").shouldBeRight()
        val departureTime = Instant.parse("2026-05-01T10:00:00Z")
        val arrivalTime = Instant.parse("2026-05-01T23:00:00Z")

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

        context("Given: 新しくフライトが登録された場合") {
            context("When: 登録イベント履歴から状態を復元すると") {
                val flightInfo = FlightInfo.replay(nonEmptyListOf(registeredEvent))

                it("Then: 定刻通りのフライト（ScheduledFlightInfo）として正しく復元され、プロパティがセットされていること") {
                    flightInfo.shouldBeTypeOf<ScheduledFlightInfo>()
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
            val activatedEvent = FlightMonitoringActivated(
                id = DomainEventId.generate(),
                aggregateId = flightIdentity,
                sequenceNumber = 1,
                meta = DomainEventMeta(Instant.now(), CorrelationId.generate())
            )

            context("When: 登録と監視活性化イベントの履歴から状態を復元すると") {
                val flightInfo = FlightInfo.replay(nonEmptyListOf(registeredEvent, activatedEvent))

                it("Then: 監視ステータスが ACTIVATED になっていること") {
                    flightInfo.monitoringStatus shouldBe MonitoringStatus.ACTIVATED
                    flightInfo.version shouldBe AggregateVersion(1)
                }
            }
        }

        context("Given: 遅延が発生したフライトの場合") {
            val newDepartureTime = Instant.parse("2026-05-01T11:00:00Z")
            val newArrivalTime = Instant.parse("2026-05-02T00:00:00Z")
            val delayedEvent = FlightDelayed(
                id = DomainEventId.generate(),
                aggregateId = flightIdentity,
                sequenceNumber = 1,
                meta = DomainEventMeta(Instant.now(), CorrelationId.generate()),
                estimatedDepartureTime = newDepartureTime,
                estimatedArrivalTime = newArrivalTime
            )

            context("When: 登録と遅延イベントの履歴から状態を復元すると") {
                val flightInfo = FlightInfo.replay(nonEmptyListOf(registeredEvent, delayedEvent))

                it("Then: 遅延フライト状態（DelayedFlightInfo）として復元され、推定時刻がセットされていること") {
                    flightInfo.shouldBeTypeOf<DelayedFlightInfo>()
                    (flightInfo as DelayedFlightInfo).estimatedDepartureTime shouldBe newDepartureTime
                    flightInfo.estimatedArrivalTime shouldBe newArrivalTime
                    flightInfo.version shouldBe AggregateVersion(1)
                }
            }
        }

        context("Given: 状況が不確実になったフライトの場合") {
            val uncertainEvent = FlightStatusUncertain(
                id = DomainEventId.generate(),
                aggregateId = flightIdentity,
                sequenceNumber = 1,
                meta = DomainEventMeta(Instant.now(), CorrelationId.generate()),
                reason = "Weather condition"
            )

            context("When: 登録と状況不確実イベントの履歴から状態を復元すると") {
                val flightInfo = FlightInfo.replay(nonEmptyListOf(registeredEvent, uncertainEvent))

                it("Then: 不確実フライト状態（UncertainFlightInfo）として復元され、理由がセットされていること") {
                    flightInfo.shouldBeTypeOf<UncertainFlightInfo>()
                    (flightInfo as UncertainFlightInfo).reason shouldBe "Weather condition"
                    flightInfo.version shouldBe AggregateVersion(1)
                }
            }
        }

        context("Given: 欠航が確定したフライトの場合") {
            val canceledEvent = FlightCanceled(
                id = DomainEventId.generate(),
                aggregateId = flightIdentity,
                sequenceNumber = 1,
                meta = DomainEventMeta(Instant.now(), CorrelationId.generate())
            )

            context("When: 登録と欠航イベントの履歴から状態を復元すると") {
                val flightInfo = FlightInfo.replay(nonEmptyListOf(registeredEvent, canceledEvent))

                it("Then: 欠航フライト状態（CanceledFlightInfo）として復元され、監視ステータスが COMPLETED になっていること") {
                    flightInfo.shouldBeTypeOf<CanceledFlightInfo>()
                    flightInfo.monitoringStatus shouldBe MonitoringStatus.COMPLETED
                    flightInfo.version shouldBe AggregateVersion(1)
                }
            }
        }

        context("Given: 到着が確定したフライトの場合") {
            val arrivedEvent = FlightArrived(
                id = DomainEventId.generate(),
                aggregateId = flightIdentity,
                sequenceNumber = 1,
                meta = DomainEventMeta(Instant.now(), CorrelationId.generate())
            )

            context("When: 登録と到着イベントの履歴から状態を復元すると") {
                val flightInfo = FlightInfo.replay(nonEmptyListOf(registeredEvent, arrivedEvent))

                it("Then: 到着済みフライト状態（ArrivedFlightInfo）として復元され、監視ステータスが COMPLETED になっていること") {
                    flightInfo.shouldBeTypeOf<ArrivedFlightInfo>()
                    flightInfo.monitoringStatus shouldBe MonitoringStatus.COMPLETED
                    flightInfo.version shouldBe AggregateVersion(1)
                }
            }
        }

        context("Given: 遅延から定刻に復帰したフライトの場合") {
            val newDepartureTime = Instant.parse("2026-05-01T11:00:00Z")
            val newArrivalTime = Instant.parse("2026-05-02T00:00:00Z")
            val delayedEvent = FlightDelayed(
                id = DomainEventId.generate(),
                aggregateId = flightIdentity,
                sequenceNumber = 1,
                meta = DomainEventMeta(Instant.now(), CorrelationId.generate()),
                estimatedDepartureTime = newDepartureTime,
                estimatedArrivalTime = newArrivalTime
            )
            val returnedEvent = FlightOnScheduleReturned(
                id = DomainEventId.generate(),
                aggregateId = flightIdentity,
                sequenceNumber = 2,
                meta = DomainEventMeta(Instant.now(), CorrelationId.generate())
            )

            context("When: 登録、遅延、定刻復帰イベントの履歴から状態を復元すると") {
                val flightInfo = FlightInfo.replay(nonEmptyListOf(registeredEvent, delayedEvent, returnedEvent))

                it("Then: 再び定刻フライト状態（ScheduledFlightInfo）として復元され、バージョンが 2 になっていること") {
                    flightInfo.shouldBeTypeOf<ScheduledFlightInfo>()
                    flightInfo.version shouldBe AggregateVersion(2)
                }
            }
        }

        context("Given: 監視完了となったフライトの場合") {
            val completedEvent = FlightMonitoringCompleted(
                id = DomainEventId.generate(),
                aggregateId = flightIdentity,
                sequenceNumber = 1,
                meta = DomainEventMeta(Instant.now(), CorrelationId.generate())
            )

            context("When: 登録と監視完了イベントの履歴から状態を復元すると") {
                val flightInfo = FlightInfo.replay(nonEmptyListOf(registeredEvent, completedEvent))

                it("Then: 監視ステータスが COMPLETED になっていること") {
                    flightInfo.monitoringStatus shouldBe MonitoringStatus.COMPLETED
                    flightInfo.version shouldBe AggregateVersion(1)
                }
            }
        }

        context("Given: 監視失敗となったフライトの場合") {
            val failedEvent = FlightMonitoringFailed(
                id = DomainEventId.generate(),
                aggregateId = flightIdentity,
                sequenceNumber = 1,
                meta = DomainEventMeta(Instant.now(), CorrelationId.generate()),
                reason = "Webhook registration failed"
            )

            context("When: 登録と監視失敗イベントの履歴から状態を復元すると") {
                val flightInfo = FlightInfo.replay(nonEmptyListOf(registeredEvent, failedEvent))

                it("Then: 監視ステータスが FAILED になっていること") {
                    flightInfo.monitoringStatus shouldBe MonitoringStatus.FAILED
                    flightInfo.version shouldBe AggregateVersion(1)
                }
            }
        }
    }
})
