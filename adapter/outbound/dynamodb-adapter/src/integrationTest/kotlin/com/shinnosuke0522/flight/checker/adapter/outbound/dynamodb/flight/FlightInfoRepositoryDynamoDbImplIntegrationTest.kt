package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.flight

import com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.config.DataDynamoDbTest
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightArrived
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightCanceled
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightDelayed
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightInfoEvent
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightInfoRegistered
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightMonitoringActivated
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightMonitoringCompleted
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightMonitoringFailed
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightOnScheduleReturned
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightStatusUncertain
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightInfo
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightPoint
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

@DataDynamoDbTest
class FlightInfoRepositoryDynamoDbImplIntegrationTest : FunSpec() {

    @Autowired
    lateinit var repository: FlightInfoRepositoryDynamoDbImpl

    init {
        extension(SpringExtension())

        val flightIdentity = FlightIdentity.create("JL123", LocalDate.of(2026, 5, 1)).getOrNull()!!
        val departurePoint = FlightPoint.create("JP", "HND", "Asia/Tokyo").getOrNull()!!
        val arrivalPoint = FlightPoint.create("US", "JFK", "America/New_York").getOrNull()!!

        context("save & findByFlightIdentity") {
            test("新しく作成した FlightInfo が保存・再構築できること") {
                val event = FlightInfoRegistered(
                    id = DomainEventId.generate(),
                    meta = DomainEventMeta.forRootEvent { java.time.Instant.now() },
                    aggregateId = flightIdentity,
                    sequenceNumber = 1L,
                    departurePoint = departurePoint,
                    arrivalPoint = arrivalPoint,
                    scheduledDepartureTime = java.time.Instant.now(),
                    scheduledArrivalTime = java.time.Instant.now().plusSeconds(3600)
                )
                val flightInfo = FlightInfo.replay(arrow.core.nonEmptyListOf(event))

                repository.save(event, flightInfo)

                val restored = repository.findByFlightIdentity(flightIdentity)
                restored shouldBe flightInfo
            }

            test("存在しない AggregateId で検索した場合は null が返ること") {
                val notFoundIdentity = FlightIdentity.create("NH999", LocalDate.of(2026, 5, 1)).getOrNull()!!
                val restored = repository.findByFlightIdentity(notFoundIdentity)
                restored shouldBe null
            }

            test("同一バージョンに対する保存は Optimistic locking で失敗すること") {
                val duplicateEvent = FlightInfoRegistered(
                    id = DomainEventId.generate(),
                    meta = DomainEventMeta.forRootEvent { java.time.Instant.now() },
                    aggregateId = flightIdentity,
                    sequenceNumber = 1L, // 既に 1L は保存済み
                    departurePoint = departurePoint,
                    arrivalPoint = arrivalPoint,
                    scheduledDepartureTime = java.time.Instant.now(),
                    scheduledArrivalTime = java.time.Instant.now().plusSeconds(3600)
                )
                val flightInfo = FlightInfo.replay(arrow.core.nonEmptyListOf(duplicateEvent))

                shouldThrow<IllegalStateException> {
                    repository.save(duplicateEvent, flightInfo)
                }
            }

            test("スナップショット保存条件外（sequenceNumber = 2L）のイベントが保存・再構築できること") {
                val flightIdentity2 = FlightIdentity.create("NH123", LocalDate.of(2026, 5, 2)).getOrNull()!!
                val event1 = FlightInfoRegistered(
                    id = DomainEventId.generate(),
                    meta = DomainEventMeta.forRootEvent { java.time.Instant.now() },
                    aggregateId = flightIdentity2,
                    sequenceNumber = 1L,
                    departurePoint = departurePoint,
                    arrivalPoint = arrivalPoint,
                    scheduledDepartureTime = java.time.Instant.now(),
                    scheduledArrivalTime = java.time.Instant.now().plusSeconds(3600)
                )
                val flightInfo1 = FlightInfo.replay(arrow.core.nonEmptyListOf(event1))
                repository.save(event1, flightInfo1)

                val event2 = com.shinnosuke0522.flight.checker.domain.flight.event.FlightDelayed(
                    id = DomainEventId.generate(),
                    meta = DomainEventMeta(
                        occurredAt = java.time.Instant.now(),
                        correlationId = event1.meta.correlationId,
                        causationId = event1.id
                    ),
                    aggregateId = flightIdentity2,
                    sequenceNumber = 2L,
                    estimatedDepartureTime = java.time.Instant.now().plusSeconds(1800),
                    estimatedArrivalTime = java.time.Instant.now().plusSeconds(5400)
                )
                val flightInfo2 = FlightInfo.replay(arrow.core.nonEmptyListOf(event1, event2))
                repository.save(event2, flightInfo2)

                val restored = repository.findByFlightIdentity(flightIdentity2)
                restored shouldBe flightInfo2
                restored?.version?.value shouldBe 2L
            }
        }

        context("Event Sourcing Lifecycle") {
            test("すべてのFlightInfoEventが正しく永続化され、Replayで最新状態が復元できること（ライフサイクルテスト）") {
                val flightIdentityLifecycle = FlightIdentity.create("LC123", LocalDate.of(2026, 6, 1)).getOrNull()!!
                val event1 = FlightInfoRegistered(
                    id = DomainEventId.generate(),
                    meta = DomainEventMeta.forRootEvent { java.time.Instant.now() },
                    aggregateId = flightIdentityLifecycle,
                    sequenceNumber = 1L,
                    departurePoint = departurePoint,
                    arrivalPoint = arrivalPoint,
                    scheduledDepartureTime = java.time.Instant.now(),
                    scheduledArrivalTime = java.time.Instant.now().plusSeconds(3600)
                )
                val events = mutableListOf<FlightInfoEvent>(
                    event1
                )
                var currentFlightInfo = FlightInfo.replay(arrow.core.nonEmptyListOf(event1))
                repository.save(event1, currentFlightInfo)
                suspend fun applyAndSave(event: FlightInfoEvent) {
                    events.add(event)
                    @Suppress("SpreadOperator")
                    currentFlightInfo = FlightInfo.replay(
                        arrow.core.nonEmptyListOf(events.first(), *events.drop(1).toTypedArray())
                    )
                    repository.save(event, currentFlightInfo)
                }
                applyAndSave(
                    FlightDelayed(
                        id = DomainEventId.generate(),
                        meta = DomainEventMeta(java.time.Instant.now(), event1.meta.correlationId, events.last().id),
                        aggregateId = flightIdentityLifecycle,
                        sequenceNumber = 2L,
                        estimatedDepartureTime = java.time.Instant.now().plusSeconds(1800),
                        estimatedArrivalTime = java.time.Instant.now().plusSeconds(5400)
                    )
                )
                applyAndSave(
                    FlightStatusUncertain(
                        id = DomainEventId.generate(),
                        meta = DomainEventMeta(java.time.Instant.now(), event1.meta.correlationId, events.last().id),
                        aggregateId = flightIdentityLifecycle,
                        sequenceNumber = 3L,
                        reason = "API Delay"
                    )
                )
                applyAndSave(
                    FlightOnScheduleReturned(
                        id = DomainEventId.generate(),
                        meta = DomainEventMeta(java.time.Instant.now(), event1.meta.correlationId, events.last().id),
                        aggregateId = flightIdentityLifecycle,
                        sequenceNumber = 4L
                    )
                )
                applyAndSave(
                    FlightMonitoringActivated(
                        id = DomainEventId.generate(),
                        meta = DomainEventMeta(java.time.Instant.now(), event1.meta.correlationId, events.last().id),
                        aggregateId = flightIdentityLifecycle,
                        sequenceNumber = 5L
                    )
                )
                applyAndSave(
                    FlightMonitoringFailed(
                        id = DomainEventId.generate(),
                        meta = DomainEventMeta(java.time.Instant.now(), event1.meta.correlationId, events.last().id),
                        aggregateId = flightIdentityLifecycle,
                        sequenceNumber = 6L,
                        reason = "API Timeout"
                    )
                )
                applyAndSave(
                    FlightMonitoringCompleted(
                        id = DomainEventId.generate(),
                        meta = DomainEventMeta(java.time.Instant.now(), event1.meta.correlationId, events.last().id),
                        aggregateId = flightIdentityLifecycle,
                        sequenceNumber = 7L
                    )
                )
                applyAndSave(
                    FlightArrived(
                        id = DomainEventId.generate(),
                        meta = DomainEventMeta(java.time.Instant.now(), event1.meta.correlationId, events.last().id),
                        aggregateId = flightIdentityLifecycle,
                        sequenceNumber = 8L
                    )
                )
                val restored = repository.findByFlightIdentity(flightIdentityLifecycle)
                restored shouldBe currentFlightInfo
                restored?.version?.value shouldBe 8L
                // FlightCanceled用にもう一つ作成
                val flightIdentityCanceled = FlightIdentity.create("LC999", LocalDate.of(2026, 6, 2)).getOrNull()!!
                val eventC1 = event1.copy(id = DomainEventId.generate(), aggregateId = flightIdentityCanceled)
                var currentCanceledInfo = FlightInfo.replay(arrow.core.nonEmptyListOf(eventC1))
                repository.save(eventC1, currentCanceledInfo)
                val eventC2 = FlightCanceled(
                    id = DomainEventId.generate(),
                    meta = DomainEventMeta(java.time.Instant.now(), eventC1.meta.correlationId, eventC1.id),
                    aggregateId = flightIdentityCanceled,
                    sequenceNumber = 2L
                )
                currentCanceledInfo = FlightInfo.replay(arrow.core.nonEmptyListOf(eventC1, eventC2))
                repository.save(eventC2, currentCanceledInfo)
                val restoredCanceled = repository.findByFlightIdentity(flightIdentityCanceled)
                restoredCanceled shouldBe currentCanceledInfo
                restoredCanceled?.version?.value shouldBe 2L
            }
        }

        context("Event Sourcing Snapshot") {
            test("SNAPSHOT_INTERVALに到達した場合（sequenceNumber = 10L）にスナップショットが作成・再構築できること") {
                val flightIdentity10 = FlightIdentity.create("BC123", LocalDate.of(2026, 5, 3)).getOrNull()!!
                val event1 = FlightInfoRegistered(
                    id = DomainEventId.generate(),
                    meta = DomainEventMeta.forRootEvent { java.time.Instant.now() },
                    aggregateId = flightIdentity10,
                    sequenceNumber = 1L,
                    departurePoint = departurePoint,
                    arrivalPoint = arrivalPoint,
                    scheduledDepartureTime = java.time.Instant.now(),
                    scheduledArrivalTime = java.time.Instant.now().plusSeconds(3600)
                )
                var currentFlightInfo = FlightInfo.replay(arrow.core.nonEmptyListOf(event1))
                repository.save(event1, currentFlightInfo)

                val events = mutableListOf<com.shinnosuke0522.flight.checker.domain.flight.event.FlightInfoEvent>(
                    event1
                )
                for (i in 2L..10L) {
                    val event = com.shinnosuke0522.flight.checker.domain.flight.event.FlightDelayed(
                        id = DomainEventId.generate(),
                        meta = DomainEventMeta(
                            occurredAt = java.time.Instant.now(),
                            correlationId = event1.meta.correlationId,
                            causationId = events.last().id
                        ),
                        aggregateId = flightIdentity10,
                        sequenceNumber = i,
                        estimatedDepartureTime = java.time.Instant.now().plusSeconds(1800 + i),
                        estimatedArrivalTime = java.time.Instant.now().plusSeconds(5400 + i)
                    )
                    events.add(event)
                    @Suppress("SpreadOperator")
                    currentFlightInfo = FlightInfo.replay(
                        arrow.core.nonEmptyListOf(events.first(), *events.drop(1).toTypedArray())
                    )
                    repository.save(event, currentFlightInfo)
                }

                val restored = repository.findByFlightIdentity(flightIdentity10)
                restored shouldBe currentFlightInfo
                restored?.version?.value shouldBe 10L
            }
        }
    }
}
