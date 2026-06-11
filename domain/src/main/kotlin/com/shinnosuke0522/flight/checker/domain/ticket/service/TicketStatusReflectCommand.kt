package com.shinnosuke0522.flight.checker.domain.ticket.service

import com.shinnosuke0522.flight.checker.domain.base.event.CorrelationId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.ticket.model.AnomalyDelayed
import com.shinnosuke0522.flight.checker.domain.ticket.model.AnomalyUncertain
import java.time.Instant

/**
 * チケットの状況を外部の事実（フライト状況）に基づいて反映するためのコマンド。
 */
sealed interface TicketStatusReflectCommand {
    val occurredAt: Instant
    val correlationId: CorrelationId
    val causationId: DomainEventId // 起因となったフライトイベントのID
}

/** 定刻・予定通りのフライト状況を反映する */
data class TicketOnScheduleReflectCommand(
    override val occurredAt: Instant,
    override val correlationId: CorrelationId,
    override val causationId: DomainEventId
) : TicketStatusReflectCommand

/** 遅延を検知した事実を反映する */
data class TicketFlightDelayedReflectCommand(
    override val occurredAt: Instant,
    override val correlationId: CorrelationId,
    override val causationId: DomainEventId,
    val detail: AnomalyDelayed
) : TicketStatusReflectCommand

/** 欠航を検知した事実を反映する */
data class TicketFlightCanceledReflectCommand(
    override val occurredAt: Instant,
    override val correlationId: CorrelationId,
    override val causationId: DomainEventId
) : TicketStatusReflectCommand

/** 動静不明・不確実を検知した事実を反映する */
data class TicketFlightUncertainReflectCommand(
    override val occurredAt: Instant,
    override val correlationId: CorrelationId,
    override val causationId: DomainEventId,
    val detail: AnomalyUncertain
) : TicketStatusReflectCommand

/** 到着（目的達成）した事実を反映する */
data class TicketArrivedReflectCommand(
    override val occurredAt: Instant,
    override val correlationId: CorrelationId,
    override val causationId: DomainEventId
) : TicketStatusReflectCommand
