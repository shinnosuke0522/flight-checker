package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.ticket

import com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.config.DataDynamoDbTest
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketAnomalyAcknowledged
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketAnomalyRecovered
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketEvent
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFinished
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFlightCanceled
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFlightDelayed
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFlightUncertain
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketRegistered
import com.shinnosuke0522.flight.checker.domain.ticket.model.AnomalyDelayed
import com.shinnosuke0522.flight.checker.domain.ticket.model.AnomalyUncertain
import com.shinnosuke0522.flight.checker.domain.ticket.model.FinishReason
import com.shinnosuke0522.flight.checker.domain.ticket.model.Ticket
import com.shinnosuke0522.flight.checker.domain.ticket.model.TicketId
import com.shinnosuke0522.flight.checker.domain.ticket.model.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

@DataDynamoDbTest
class TicketRepositoryDynamoDbImplIntegrationTest : FunSpec() {

    @Autowired
    lateinit var repository: TicketRepositoryDynamoDbImpl

    init {
        extension(SpringExtension())

        val ticketId = TicketId.generate()
        val userId = UserId.generate()
        val flightIdentity = FlightIdentity.create(
            rawFlightCode = "JL123",
            departureDate = LocalDate.of(2026, 5, 1)
        ).getOrNull()!!

        context("save & findById") {
            test("新しく作成した Ticket が保存・再構築できること") {
                val event = TicketRegistered(
                    id = DomainEventId.generate(),
                    meta = DomainEventMeta.forRootEvent { java.time.Instant.now() },
                    aggregateId = ticketId,
                    sequenceNumber = 1L,
                    userId = userId,
                    flightIdentity = flightIdentity
                )
                val ticket = Ticket.replay(arrow.core.nonEmptyListOf(event))

                repository.save(event, ticket)

                val restored = repository.findById(ticketId)
                restored shouldBe ticket
            }

            test("存在しない AggregateId で検索した場合は null が返ること") {
                val notFoundId = TicketId.generate()
                val restored = repository.findById(notFoundId)
                restored shouldBe null
            }

            test("同一バージョンに対する保存は Optimistic locking で失敗すること") {
                val duplicateEvent = TicketRegistered(
                    id = DomainEventId.generate(),
                    meta = DomainEventMeta.forRootEvent { java.time.Instant.now() },
                    aggregateId = ticketId,
                    sequenceNumber = 1L, // 既に 1L は保存済み
                    userId = userId,
                    flightIdentity = flightIdentity
                )
                val ticket = Ticket.replay(arrow.core.nonEmptyListOf(duplicateEvent))

                shouldThrow<IllegalStateException> {
                    repository.save(duplicateEvent, ticket)
                }
            }

            test("スナップショット保存条件外（sequenceNumber = 2L）のイベントが保存・再構築できること") {
                val ticketId2 = TicketId.generate()
                val userId2 = UserId.generate()
                val flightIdentity2 = FlightIdentity.create("BC123", LocalDate.of(2026, 5, 2)).getOrNull()!!
                val event1 = TicketRegistered(
                    id = DomainEventId.generate(),
                    meta = DomainEventMeta.forRootEvent { java.time.Instant.now() },
                    aggregateId = ticketId2,
                    sequenceNumber = 1L,
                    userId = userId2,
                    flightIdentity = flightIdentity2
                )
                val ticket1 = Ticket.replay(arrow.core.nonEmptyListOf(event1))
                repository.save(event1, ticket1)

                val event2 = TicketFlightDelayed(
                    id = DomainEventId.generate(),
                    meta = DomainEventMeta(
                        occurredAt = java.time.Instant.now(),
                        correlationId = event1.meta.correlationId,
                        causationId = event1.id
                    ),
                    aggregateId = ticketId2,
                    sequenceNumber = 2L,
                    detail = AnomalyDelayed(
                        java.time.Instant.now().plusSeconds(1800).toString()
                    )
                )
                val ticket2 = Ticket.replay(arrow.core.nonEmptyListOf(event1, event2))
                repository.save(event2, ticket2)

                val restored = repository.findById(ticketId2)
                restored shouldBe ticket2
                restored?.version?.value shouldBe 2L
            }
        }

        context("Event Sourcing Lifecycle") {
            test("すべてのTicketEventが正しく永続化され、Replayで最新状態が復元できること（ライフサイクルテスト）") {
                val ticketIdLifecycle = TicketId.generate()
                val userIdLifecycle = UserId.generate()
                val flightIdentityLifecycle = FlightIdentity.create("LC123", LocalDate.of(2026, 6, 1)).getOrNull()!!

                val event1 = TicketRegistered(
                    id = DomainEventId.generate(),
                    meta = DomainEventMeta.forRootEvent { java.time.Instant.now() },
                    aggregateId = ticketIdLifecycle,
                    sequenceNumber = 1L,
                    userId = userIdLifecycle,
                    flightIdentity = flightIdentityLifecycle
                )
                val events = mutableListOf<TicketEvent>(event1)
                var currentTicket = Ticket.replay(arrow.core.nonEmptyListOf(event1))
                repository.save(event1, currentTicket)

                suspend fun applyAndSave(event: TicketEvent) {
                    events.add(event)
                    @Suppress("SpreadOperator")
                    currentTicket = Ticket.replay(
                        arrow.core.nonEmptyListOf(events.first(), *events.drop(1).toTypedArray())
                    )
                    repository.save(event, currentTicket)
                }

                applyAndSave(
                    TicketFlightDelayed(
                        id = DomainEventId.generate(),
                        meta = DomainEventMeta(java.time.Instant.now(), event1.meta.correlationId, events.last().id),
                        aggregateId = ticketIdLifecycle,
                        sequenceNumber = 2L,
                        detail = AnomalyDelayed(
                            java.time.Instant.now().plusSeconds(1800).toString()
                        )
                    )
                )

                applyAndSave(
                    TicketAnomalyAcknowledged(
                        id = DomainEventId.generate(),
                        meta = DomainEventMeta(java.time.Instant.now(), event1.meta.correlationId, events.last().id),
                        aggregateId = ticketIdLifecycle,
                        sequenceNumber = 3L,
                        acknowledgedAnomaly = AnomalyDelayed(
                            java.time.Instant.now().plusSeconds(1800).toString()
                        )
                    )
                )

                applyAndSave(
                    TicketAnomalyRecovered(
                        id = DomainEventId.generate(),
                        meta = DomainEventMeta(java.time.Instant.now(), event1.meta.correlationId, events.last().id),
                        aggregateId = ticketIdLifecycle,
                        sequenceNumber = 4L
                    )
                )

                applyAndSave(
                    TicketFlightUncertain(
                        id = DomainEventId.generate(),
                        meta = DomainEventMeta(java.time.Instant.now(), event1.meta.correlationId, events.last().id),
                        aggregateId = ticketIdLifecycle,
                        sequenceNumber = 5L,
                        detail = AnomalyUncertain(
                            "API Delay"
                        )
                    )
                )

                applyAndSave(
                    TicketFlightCanceled(
                        id = DomainEventId.generate(),
                        meta = DomainEventMeta(java.time.Instant.now(), event1.meta.correlationId, events.last().id),
                        aggregateId = ticketIdLifecycle,
                        sequenceNumber = 6L
                    )
                )

                applyAndSave(
                    TicketFinished(
                        id = DomainEventId.generate(),
                        meta = DomainEventMeta(java.time.Instant.now(), event1.meta.correlationId, events.last().id),
                        aggregateId = ticketIdLifecycle,
                        sequenceNumber = 7L,
                        reason = FinishReason.CANCELED_ACCEPTED
                    )
                )

                val restored = repository.findById(ticketIdLifecycle)
                restored shouldBe currentTicket
                restored?.version?.value shouldBe 7L
            }
        }

        context("Event Sourcing Snapshot") {
            test("SNAPSHOT_INTERVALに到達した場合（sequenceNumber = 10L）にスナップショットが作成・再構築できること") {
                val ticketId10 = TicketId.generate()
                val userId10 = UserId.generate()
                val flightIdentity10 = FlightIdentity.create("ZZ999", LocalDate.of(2026, 5, 3)).getOrNull()!!
                val event1 = TicketRegistered(
                    id = DomainEventId.generate(),
                    meta = DomainEventMeta.forRootEvent { java.time.Instant.now() },
                    aggregateId = ticketId10,
                    sequenceNumber = 1L,
                    userId = userId10,
                    flightIdentity = flightIdentity10
                )
                var currentTicket = Ticket.replay(arrow.core.nonEmptyListOf(event1))
                repository.save(event1, currentTicket)

                val events = mutableListOf<TicketEvent>(
                    event1
                )
                for (i in 2L..10L) {
                    val event = TicketFlightDelayed(
                        id = DomainEventId.generate(),
                        meta = DomainEventMeta(
                            occurredAt = java.time.Instant.now(),
                            correlationId = event1.meta.correlationId,
                            causationId = events.last().id
                        ),
                        aggregateId = ticketId10,
                        sequenceNumber = i,
                        detail = AnomalyDelayed(
                            java.time.Instant.now().plusSeconds(1800 + i).toString()
                        )
                    )
                    events.add(event)
                    @Suppress("SpreadOperator")
                    currentTicket = Ticket.replay(
                        arrow.core.nonEmptyListOf(events.first(), *events.drop(1).toTypedArray())
                    )
                    repository.save(event, currentTicket)
                }

                val restored = repository.findById(ticketId10)
                restored shouldBe currentTicket
                restored?.version?.value shouldBe 10L
            }
        }

        context("findByUserId") {
            test("指定した UserId に紐づく Ticket が取得できること") {
                val tickets = repository.findByUserId(userId)
                tickets.shouldHaveSize(1)
                tickets[0].id shouldBe ticketId
            }

            test("存在しない UserId で検索した場合は空リストが返ること") {
                val notFoundUserId = UserId.generate()
                val tickets = repository.findByUserId(notFoundUserId)
                tickets.shouldHaveSize(0)
            }
        }

        context("findByFlightIdentity") {
            test("指定した FlightIdentity に紐づく Ticket が取得できること") {
                val tickets = repository.findByFlightIdentity(flightIdentity)
                tickets.shouldHaveSize(1)
                tickets[0].id shouldBe ticketId
            }

            test("存在しない FlightIdentity で検索した場合は空リストが返ること") {
                val notFoundFlightIdentity = FlightIdentity.create("NH999", LocalDate.of(2026, 5, 1)).getOrNull()!!
                val tickets = repository.findByFlightIdentity(notFoundFlightIdentity)
                tickets.shouldHaveSize(0)
            }
        }
    }
}
