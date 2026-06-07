package com.shinnosuke0522.flight.checker.domain.ticket.model

import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketAnomalyAcknowledged
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketAnomalyDetected
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketAnomalyRecovered
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFinished
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Instant
import java.time.LocalDate

class TicketTest : DescribeSpec({
    val userId = UserId.generate()
    val flightIdentity = FlightIdentity.create("JL123", LocalDate.of(2026, 6, 7)).getOrNull()!!
    val ticketId = TicketId.generate()
    val now = Instant.now()

    describe("Ticket state transitions via apply") {

        context("Given: A newly registered NormalTicket") {
            val ticket = Ticket.initial(ticketId, userId, flightIdentity)

            context("When: TicketAnomalyDetected event is applied") {
                val event = TicketAnomalyDetected(
                    id = DomainEventId.generate(),
                    aggregateId = ticketId,
                    sequenceNumber = 2,
                    meta = DomainEventMeta.forRootEvent { now },
                    statusSummary = "DELAYED:10:00"
                )
                val result = ticket.apply(event)

                it("Then: should transition to AlertTicket with the status summary") {
                    result.shouldBeInstanceOf<AlertTicket>()
                    result.currentStatusSummary shouldBe "DELAYED:10:00"
                    result.version shouldBe AggregateVersion(2)
                }
            }
        }

        context("Given: An AlertTicket") {
            val ticket = AlertTicket(
                id = ticketId,
                version = AggregateVersion(2),
                userId = userId,
                flightIdentity = flightIdentity,
                currentStatusSummary = "CANCELED"
            )

            context("When: TicketAnomalyAcknowledged event is applied") {
                val event = TicketAnomalyAcknowledged(
                    id = DomainEventId.generate(),
                    aggregateId = ticketId,
                    sequenceNumber = 3,
                    meta = DomainEventMeta.forRootEvent { now },
                    acknowledgedStatusSummary = "CANCELED"
                )
                val result = ticket.apply(event)

                it("Then: should return to NormalTicket with the acknowledged status remembered") {
                    result.shouldBeInstanceOf<NormalTicket>()
                    result.acknowledgedStatusSummary shouldBe "CANCELED"
                    result.version shouldBe AggregateVersion(3)
                }
            }

            context("When: TicketAnomalyRecovered event is applied") {
                val event = TicketAnomalyRecovered(
                    id = DomainEventId.generate(),
                    aggregateId = ticketId,
                    sequenceNumber = 3,
                    meta = DomainEventMeta.forRootEvent { now }
                )
                val result = ticket.apply(event)

                it("Then: should return to NormalTicket and reset acknowledged status to null") {
                    result.shouldBeInstanceOf<NormalTicket>()
                    result.acknowledgedStatusSummary shouldBe null
                }
            }
        }

        context("Given: A FinishedTicket (Terminal State)") {
            val ticket = FinishedTicket(
                id = ticketId,
                version = AggregateVersion(2),
                userId = userId,
                flightIdentity = flightIdentity,
                reason = FinishReason.ARRIVED
            )

            context("When: Any event is applied") {
                val event = TicketAnomalyDetected(
                    id = DomainEventId.generate(),
                    aggregateId = ticketId,
                    sequenceNumber = 3,
                    meta = DomainEventMeta.forRootEvent { now },
                    statusSummary = "NEW_ANOMALY"
                )
                val result = ticket.apply(event)

                it("Then: should remain in FinishedTicket state without changing version") {
                    result.shouldBeInstanceOf<FinishedTicket>()
                    result.reason shouldBe FinishReason.ARRIVED
                    result.version shouldBe AggregateVersion(2)
                }
            }
        }
    }
})
