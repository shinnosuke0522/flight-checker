package com.shinnosuke0522.flight.checker.domain.ticket.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.shinnosuke0522.flight.checker.domain.base.event.CorrelationId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.ticket.error.TicketError
import com.shinnosuke0522.flight.checker.domain.ticket.error.TicketNotAlertStateError
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketAnomalyAcknowledged
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketEvent
import com.shinnosuke0522.flight.checker.domain.ticket.model.AlertTicket
import com.shinnosuke0522.flight.checker.domain.ticket.model.Ticket
import java.time.Instant

data class TicketAcknowledgeCommand(
    val occurredAt: Instant,
    val correlationId: CorrelationId,
    val causationId: DomainEventId
)

/**
 * ユーザーがチケットの異常（遅延など）を認知・承諾したことを処理するドメインサービス。
 */
object TicketAnomalyAcknowledger {

    fun acknowledge(
        ticket: Ticket,
        command: TicketAcknowledgeCommand
    ): Either<TicketError, Pair<Ticket, TicketEvent>> = either {
        ensure(ticket is AlertTicket) { TicketNotAlertStateError(ticket.id) }

        val event = TicketAnomalyAcknowledged(
            id = DomainEventId.generate(),
            aggregateId = ticket.id,
            sequenceNumber = ticket.version.nextVersion().value,
            meta = DomainEventMeta.forCausedEvent(
                clock = { command.occurredAt },
                correlationId = command.correlationId,
                causationId = command.causationId
            ),
            acknowledgedAnomaly = ticket.currentAnomaly
        )

        Pair(ticket.apply(event), event)
    }
}
