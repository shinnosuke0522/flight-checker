package com.shinnosuke0522.flight.checker.domain.ticket.service

import com.shinnosuke0522.flight.checker.domain.base.event.CorrelationId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.ticket.model.AnomalyDelayed
import com.shinnosuke0522.flight.checker.domain.ticket.model.AnomalyUncertain
import java.time.Instant

/**
 * チケットの状況を外部の事実（フライト状況）と同期するためのコマンド。
 */
sealed interface TicketSyncCommand {
    val occurredAt: Instant
    val correlationId: CorrelationId
    val causationId: DomainEventId // 起因となったフライトイベントのID
}

/** 定刻・予定通りのフライト状況を受け取った事実 */
data class TicketSyncOnScheduleCommand(
    override val occurredAt: Instant,
    override val correlationId: CorrelationId,
    override val causationId: DomainEventId
) : TicketSyncCommand

/** 遅延を検知した事実 */
data class TicketSyncFlightDelayedCommand(
    override val occurredAt: Instant,
    override val correlationId: CorrelationId,
    override val causationId: DomainEventId,
    val detail: AnomalyDelayed
) : TicketSyncCommand

/** 欠航を検知した事実 */
data class TicketSyncFlightCanceledCommand(
    override val occurredAt: Instant,
    override val correlationId: CorrelationId,
    override val causationId: DomainEventId
) : TicketSyncCommand

/** 動静不明・不確実を検知した事実 */
data class TicketSyncFlightUncertainCommand(
    override val occurredAt: Instant,
    override val correlationId: CorrelationId,
    override val causationId: DomainEventId,
    val detail: AnomalyUncertain
) : TicketSyncCommand

/** 到着（目的達成）した事実 */
data class TicketSyncArrivedCommand(
    override val occurredAt: Instant,
    override val correlationId: CorrelationId,
    override val causationId: DomainEventId
) : TicketSyncCommand
