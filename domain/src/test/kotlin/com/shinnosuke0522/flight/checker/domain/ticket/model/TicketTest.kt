package com.shinnosuke0522.flight.checker.domain.ticket.model

import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketAnomalyAcknowledged
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketAnomalyRecovered
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketEvent
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFinished
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFlightCanceled
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFlightDelayed
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketRegistered
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.engine.names.WithDataTestName
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.LocalDate

data class TicketApplyTestCase(
    val name: String,
    val initialTicket: Ticket,
    val event: TicketEvent,
    val expectedTicket: Ticket
) : WithDataTestName {
    override fun dataTestName() = name
}

data class TicketReplayTestCase(
    val name: String,
    val additionalEvents: List<TicketEvent>,
    val expectedTicket: Ticket
) : WithDataTestName {
    override fun dataTestName() = name
}
class TicketTest : FunSpec() {
    init {
        context("イベントの適用 (Apply) による状態遷移が正しく行われること") {
            withData(
                TicketApplyTestCase(
                    name = "登録直後のNormalTicketに遅延イベントを適用すると、AlertTicketに遷移し遅延詳細が保持される",
                    initialTicket = Ticket.initial(ticketId, userId, flightIdentity),
                    event = TicketFlightDelayed(
                        id = DomainEventId.generate(),
                        aggregateId = ticketId,
                        sequenceNumber = 2,
                        meta = DomainEventMeta.forRootEvent { now },
                        detail = delay
                    ),
                    expectedTicket = AlertTicket(
                        id = ticketId,
                        version = AggregateVersion(2),
                        userId = userId,
                        flightIdentity = flightIdentity,
                        currentAnomaly = delay
                    )
                ),
                TicketApplyTestCase(
                    name = "AlertTicketに異常承諾イベントを適用すると、AcknowledgedTicketに遷移し異常内容が保持される",
                    initialTicket = AlertTicket(
                        id = ticketId,
                        version = AggregateVersion(2),
                        userId = userId,
                        flightIdentity = flightIdentity,
                        currentAnomaly = initialAnomaly
                    ),
                    event = TicketAnomalyAcknowledged(
                        id = DomainEventId.generate(),
                        aggregateId = ticketId,
                        sequenceNumber = 3,
                        meta = DomainEventMeta.forRootEvent { now },
                        acknowledgedAnomaly = initialAnomaly
                    ),
                    expectedTicket = AcknowledgedTicket(
                        id = ticketId,
                        version = AggregateVersion(3),
                        userId = userId,
                        flightIdentity = flightIdentity,
                        acknowledgedAnomaly = initialAnomaly
                    )
                ),
                TicketApplyTestCase(
                    name = "AlertTicketに異常復旧イベントを適用すると、NormalTicketに戻る",
                    initialTicket = AlertTicket(
                        id = ticketId,
                        version = AggregateVersion(2),
                        userId = userId,
                        flightIdentity = flightIdentity,
                        currentAnomaly = initialAnomaly
                    ),
                    event = TicketAnomalyRecovered(
                        id = DomainEventId.generate(),
                        aggregateId = ticketId,
                        sequenceNumber = 3,
                        meta = DomainEventMeta.forRootEvent { now }
                    ),
                    expectedTicket = NormalTicket(
                        id = ticketId,
                        version = AggregateVersion(3),
                        userId = userId,
                        flightIdentity = flightIdentity
                    )
                )
            ) { testCase ->
                val result = testCase.initialTicket.apply(testCase.event)
                result shouldBe testCase.expectedTicket
            }
        }

        test("FinishedTicket (終端状態) にイベントを適用しようとすると、IllegalStateExceptionがスローされること") {
            val ticket = FinishedTicket(
                id = ticketId,
                version = AggregateVersion(2),
                userId = userId,
                flightIdentity = flightIdentity,
                reason = FinishReason.ARRIVED
            )
            val event = TicketFlightCanceled(
                id = DomainEventId.generate(),
                aggregateId = ticketId,
                sequenceNumber = 3,
                meta = DomainEventMeta.forRootEvent { now }
            )

            shouldThrow<IllegalStateException> {
                ticket.apply(event)
            }
        }

        context("イベント履歴から各状態を復元 (Replay) できること") {
            withData(
                TicketReplayTestCase(
                    name = "NormalTicket に復元できること",
                    additionalEvents = emptyList(),
                    expectedTicket = NormalTicket(
                        id = ticketId,
                        version = AggregateVersion(0),
                        userId = userId,
                        flightIdentity = flightIdentity
                    )
                ),
                TicketReplayTestCase(
                    name = "AlertTicket に復元できること",
                    additionalEvents = listOf(
                        TicketFlightDelayed(
                            id = DomainEventId.generate(),
                            aggregateId = ticketId,
                            sequenceNumber = 1,
                            meta = DomainEventMeta.forRootEvent { now },
                            detail = delay
                        )
                    ),
                    expectedTicket = AlertTicket(
                        id = ticketId,
                        version = AggregateVersion(1),
                        userId = userId,
                        flightIdentity = flightIdentity,
                        currentAnomaly = delay
                    )
                ),
                TicketReplayTestCase(
                    name = "AcknowledgedTicket に復元できること",
                    additionalEvents = listOf(
                        TicketFlightCanceled(
                            id = DomainEventId.generate(),
                            aggregateId = ticketId,
                            sequenceNumber = 1,
                            meta = DomainEventMeta.forRootEvent { now }
                        ),
                        TicketAnomalyAcknowledged(
                            id = DomainEventId.generate(),
                            aggregateId = ticketId,
                            sequenceNumber = 2,
                            meta = DomainEventMeta.forRootEvent { now },
                            acknowledgedAnomaly = initialAnomaly
                        )
                    ),
                    expectedTicket = AcknowledgedTicket(
                        id = ticketId,
                        version = AggregateVersion(2),
                        userId = userId,
                        flightIdentity = flightIdentity,
                        acknowledgedAnomaly = initialAnomaly
                    )
                ),
                TicketReplayTestCase(
                    name = "FinishedTicket に復元できること",
                    additionalEvents = listOf(
                        TicketFinished(
                            id = DomainEventId.generate(),
                            aggregateId = ticketId,
                            sequenceNumber = 1,
                            meta = DomainEventMeta.forRootEvent { now },
                            reason = FinishReason.ARRIVED
                        )
                    ),
                    expectedTicket = FinishedTicket(
                        id = ticketId,
                        version = AggregateVersion(1),
                        userId = userId,
                        flightIdentity = flightIdentity,
                        reason = FinishReason.ARRIVED
                    )
                )
            ) { testCase ->
                val events = nonEmptyListOf(registeredEvent, *testCase.additionalEvents.toTypedArray())
                val ticket = Ticket.replay(events)
                ticket shouldBe testCase.expectedTicket
            }
        }
    }

    companion object {
        val userId = UserId.generate()
        val flightIdentity = FlightIdentity.create("JL123", LocalDate.of(2026, 6, 7)).getOrElse { error(it) }
        val ticketId = TicketId.generate()
        val now: Instant = Instant.now()
        val delay = AnomalyDelayed("10:00")
        val initialAnomaly = AnomalyCanceled

        val registeredEvent = TicketRegistered(
            id = DomainEventId.generate(),
            aggregateId = ticketId,
            sequenceNumber = 0,
            meta = DomainEventMeta.forRootEvent { now },
            userId = userId,
            flightIdentity = flightIdentity
        )
    }
}
