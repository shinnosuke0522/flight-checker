package com.shinnosuke0522.flight.checker.domain.ticket.model

import arrow.core.getOrElse
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketAnomalyAcknowledged
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketAnomalyRecovered
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFlightCanceled
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFlightDelayed
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Instant
import java.time.LocalDate

class TicketTest : DescribeSpec({
    val userId = UserId.generate()
    val flightIdentity = FlightIdentity.create("JL123", LocalDate.of(2026, 6, 7)).getOrElse { error(it) }
    val ticketId = TicketId.generate()
    val now = Instant.now()

    describe("Ticket state transitions via apply") {

        context("Given: A newly registered NormalTicket") {
            val ticket = Ticket.initial(ticketId, userId, flightIdentity)

            context("When: TicketFlightDelayed event is applied") {
                val delay = AnomalyDelayed("10:00")
                val event = TicketFlightDelayed(
                    id = DomainEventId.generate(),
                    aggregateId = ticketId,
                    sequenceNumber = 2,
                    meta = DomainEventMeta.forRootEvent { now },
                    detail = delay
                )
                val result = ticket.apply(event)

                it("Then: should transition to AlertTicket with the delay detail") {
                    result.shouldBeInstanceOf<AlertTicket>()
                    (result as AlertTicket).currentAnomaly shouldBe delay
                    result.version shouldBe AggregateVersion(2)
                }
            }
        }

        context("Given: An AlertTicket") {
            val initialAnomaly = AnomalyCanceled
            val ticket = AlertTicket(
                id = ticketId,
                version = AggregateVersion(2),
                userId = userId,
                flightIdentity = flightIdentity,
                currentAnomaly = initialAnomaly
            )

            context("When: TicketAnomalyAcknowledged event is applied") {
                val event = TicketAnomalyAcknowledged(
                    id = DomainEventId.generate(),
                    aggregateId = ticketId,
                    sequenceNumber = 3,
                    meta = DomainEventMeta.forRootEvent { now },
                    acknowledgedAnomaly = initialAnomaly
                )
                val result = ticket.apply(event)

                it("Then: should transition to AcknowledgedTicket with the anomaly remembered") {
                    result.shouldBeInstanceOf<AcknowledgedTicket>()
                    (result as AcknowledgedTicket).acknowledgedAnomaly shouldBe initialAnomaly
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

                it("Then: should return to NormalTicket") {
                    result.shouldBeInstanceOf<NormalTicket>()
                    result.version shouldBe AggregateVersion(3)
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
                val event = TicketFlightCanceled(
                    id = DomainEventId.generate(),
                    aggregateId = ticketId,
                    sequenceNumber = 3,
                    meta = DomainEventMeta.forRootEvent { now }
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
