package com.shinnosuke0522.flight.checker.domain.ticket.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.ticket.error.TicketAlreadyFinishedError
import com.shinnosuke0522.flight.checker.domain.ticket.error.TicketAlreadyOnScheduleError
import com.shinnosuke0522.flight.checker.domain.ticket.error.TicketAnomalyAlreadySynchronizedError
import com.shinnosuke0522.flight.checker.domain.ticket.error.TicketError
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketAnomalyRecovered
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketEvent
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFinished
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFlightCanceled
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFlightDelayed
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFlightUncertain
import com.shinnosuke0522.flight.checker.domain.ticket.model.AcknowledgedTicket
import com.shinnosuke0522.flight.checker.domain.ticket.model.AlertTicket
import com.shinnosuke0522.flight.checker.domain.ticket.model.Anomaly
import com.shinnosuke0522.flight.checker.domain.ticket.model.AnomalyCanceled
import com.shinnosuke0522.flight.checker.domain.ticket.model.FinishReason
import com.shinnosuke0522.flight.checker.domain.ticket.model.FinishedTicket
import com.shinnosuke0522.flight.checker.domain.ticket.model.Ticket

/**
 * 外部の事実に基づき、[Ticket] の状態を更新するドメインサービス。
 */
object TicketStatusReflector {

    /**
     * チケットに対し、最新の状況を反映させる必要があるか判断し、適切な処理へ振り分ける。
     */
    fun reflect(
        ticket: Ticket,
        command: TicketSyncCommand
    ): Either<TicketError, Pair<Ticket, TicketEvent>> = either {
        // 終端状態ガード: 終了済みのチケットはいかなる状況反映も受け付けない
        ensure(ticket !is FinishedTicket) { TicketAlreadyFinishedError(ticket.id) }

        when (command) {
            is TicketSyncArrivedCommand -> reflectArrival(ticket, command).bind()
            is TicketSyncFlightDelayedCommand -> reflectDelayed(ticket, command).bind()
            is TicketSyncFlightCanceledCommand -> reflectCanceled(ticket, command).bind()
            is TicketSyncFlightUncertainCommand -> reflectUncertain(ticket, command).bind()
            is TicketSyncOnScheduleCommand -> reflectReturnToSchedule(ticket, command).bind()
        }
    }

    /**
     * フライトの到着（目的達成）をチケットに反映する。
     */
    fun reflectArrival(
        ticket: Ticket,
        command: TicketSyncArrivedCommand
    ): Either<TicketError, Pair<Ticket, TicketEvent>> = either {
        ensure(ticket !is FinishedTicket) { TicketAlreadyFinishedError(ticket.id) }

        val event = TicketFinished(
            id = DomainEventId.generate(),
            aggregateId = ticket.id,
            sequenceNumber = ticket.version.nextVersion().value,
            meta = createMeta(command),
            reason = FinishReason.ARRIVED
        )
        Pair(ticket.apply(event), event)
    }

    /**
     * フライトの遅延をチケットに反映する。
     */
    fun reflectDelayed(
        ticket: Ticket,
        command: TicketSyncFlightDelayedCommand
    ): Either<TicketError, Pair<Ticket, TicketEvent>> = either {
        ensure(ticket !is FinishedTicket) { TicketAlreadyFinishedError(ticket.id) }
        val newAnomaly = command.detail

        ensure(shouldUpdate(ticket, newAnomaly)) {
            TicketAnomalyAlreadySynchronizedError(ticket.id, newAnomaly)
        }

        val event = TicketFlightDelayed(
            id = DomainEventId.generate(),
            aggregateId = ticket.id,
            sequenceNumber = ticket.version.nextVersion().value,
            meta = createMeta(command),
            detail = newAnomaly
        )
        Pair(ticket.apply(event), event)
    }

    /**
     * フライトの欠航をチケットに反映する。
     */
    fun reflectCanceled(
        ticket: Ticket,
        command: TicketSyncFlightCanceledCommand
    ): Either<TicketError, Pair<Ticket, TicketEvent>> = either {
        ensure(ticket !is FinishedTicket) { TicketAlreadyFinishedError(ticket.id) }
        val newAnomaly = AnomalyCanceled

        ensure(shouldUpdate(ticket, newAnomaly)) {
            TicketAnomalyAlreadySynchronizedError(ticket.id, newAnomaly)
        }

        val event = TicketFlightCanceled(
            id = DomainEventId.generate(),
            aggregateId = ticket.id,
            sequenceNumber = ticket.version.nextVersion().value,
            meta = createMeta(command)
        )
        Pair(ticket.apply(event), event)
    }

    /**
     * フライトの動静不透明（不確実）をチケットに反映する。
     */
    fun reflectUncertain(
        ticket: Ticket,
        command: TicketSyncFlightUncertainCommand
    ): Either<TicketError, Pair<Ticket, TicketEvent>> = either {
        ensure(ticket !is FinishedTicket) { TicketAlreadyFinishedError(ticket.id) }
        val newAnomaly = command.detail

        ensure(shouldUpdate(ticket, newAnomaly)) {
            TicketAnomalyAlreadySynchronizedError(ticket.id, newAnomaly)
        }

        val event = TicketFlightUncertain(
            id = DomainEventId.generate(),
            aggregateId = ticket.id,
            sequenceNumber = ticket.version.nextVersion().value,
            meta = createMeta(command),
            detail = newAnomaly
        )
        Pair(ticket.apply(event), event)
    }

    /**
     * フライトが予定通り（定刻）に戻ったことをチケットに反映する。
     *
     * 異常状態からの復帰であれば、復旧イベントを発行する。
     */
    fun reflectReturnToSchedule(
        ticket: Ticket,
        command: TicketSyncOnScheduleCommand
    ): Either<TicketError, Pair<Ticket, TicketEvent>> = either {
        ensure(ticket !is FinishedTicket) { TicketAlreadyFinishedError(ticket.id) }

        // 異常状態（未承諾・承諾済み問わず）から正常に戻った場合のみ、復旧イベントを発行する
        ensure(ticket is AlertTicket || ticket is AcknowledgedTicket) {
            TicketAlreadyOnScheduleError(ticket.id)
        }

        val event = TicketAnomalyRecovered(
            id = DomainEventId.generate(),
            aggregateId = ticket.id,
            sequenceNumber = ticket.version.nextVersion().value,
            meta = createMeta(command)
        )
        Pair(ticket.apply(event), event)
    }

    /**
     * 新しい異常事実に基づいて、チケットの状態を更新すべきか（通知すべきか）判定する。
     */
    private fun shouldUpdate(ticket: Ticket, newAnomaly: Anomaly): Boolean {
        val currentAnomaly = when (ticket) {
            is AlertTicket -> ticket.currentAnomaly
            is AcknowledgedTicket -> ticket.acknowledgedAnomaly
            else -> null
        }
        // 現在の異常と新しい異常が完全に一致する場合のみ、更新を抑制する。
        // 内容が少しでも変わっていれば（例：遅延時間が更新された）、新しい事実として受け入れる。
        return currentAnomaly != newAnomaly
    }

    private fun createMeta(command: TicketSyncCommand): DomainEventMeta = DomainEventMeta.forCausedEvent(
        clock = { command.occurredAt },
        correlationId = command.correlationId,
        causationId = command.causationId
    )
}
