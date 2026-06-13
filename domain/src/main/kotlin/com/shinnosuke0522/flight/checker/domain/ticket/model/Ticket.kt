package com.shinnosuke0522.flight.checker.domain.ticket.model

import arrow.core.NonEmptyList
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.base.model.EventSourcingAggregateRoot
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketAnomalyAcknowledged
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketAnomalyRecovered
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketEvent
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFinished
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFlightCanceled
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFlightDelayed
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFlightUncertain
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketRegistered

/**
 * ユーザーの特定のフライトに対する関心（監視対象チケット）を表す集約ルート。
 */
sealed class Ticket : EventSourcingAggregateRoot<TicketId, TicketEvent, Ticket> {
    abstract val userId: UserId
    abstract val flightIdentity: FlightIdentity

    override fun apply(event: TicketEvent): Ticket = when (this) {
        is FinishedTicket -> this // 終端状態パターン: 終了済みなら状態は変わらない
        is NormalTicket, is AlertTicket, is AcknowledgedTicket -> when (event) {
            is TicketRegistered -> NormalTicket(
                id = event.aggregateId,
                version = AggregateVersion(event.sequenceNumber),
                userId = event.userId,
                flightIdentity = event.flightIdentity
            )

            is TicketFlightDelayed -> AlertTicket(
                id = id,
                version = AggregateVersion(event.sequenceNumber),
                userId = userId,
                flightIdentity = flightIdentity,
                currentAnomaly = event.detail
            )

            is TicketFlightCanceled -> AlertTicket(
                id = id,
                version = AggregateVersion(event.sequenceNumber),
                userId = userId,
                flightIdentity = flightIdentity,
                currentAnomaly = AnomalyCanceled
            )

            is TicketFlightUncertain -> AlertTicket(
                id = id,
                version = AggregateVersion(event.sequenceNumber),
                userId = userId,
                flightIdentity = flightIdentity,
                currentAnomaly = event.detail
            )

            is TicketAnomalyRecovered -> NormalTicket(
                id = id,
                version = AggregateVersion(event.sequenceNumber),
                userId = userId,
                flightIdentity = flightIdentity
            )

            is TicketAnomalyAcknowledged -> AcknowledgedTicket(
                id = id,
                version = AggregateVersion(event.sequenceNumber),
                userId = userId,
                flightIdentity = flightIdentity,
                acknowledgedAnomaly = event.acknowledgedAnomaly
            )

            is TicketFinished -> FinishedTicket(
                id = id,
                version = AggregateVersion(event.sequenceNumber),
                userId = userId,
                flightIdentity = flightIdentity,
                reason = event.reason
            )
        }
    }

    companion object {
        /**
         * イベント履歴から集約の状態を復元する。
         *
         * 主にリポジトリ層での復元に使用される。
         */
        fun replay(events: NonEmptyList<TicketEvent>): Ticket {
            val firstEvent = events.head
            require(firstEvent is TicketRegistered) {
                "Replay must start with TicketRegistered event, but was ${firstEvent::class.simpleName}"
            }
            val initial = NormalTicket(
                id = firstEvent.aggregateId,
                version = AggregateVersion(0),
                userId = firstEvent.userId,
                flightIdentity = firstEvent.flightIdentity
            )
            return events.fold(initial as Ticket) { ticket, event ->
                ticket.apply(event)
            }
        }

        /**
         * テスト用の初期状態（登録直後）を生成する。
         */
        fun initial(
            id: TicketId,
            userId: UserId,
            flightIdentity: FlightIdentity
        ): Ticket = NormalTicket(
            id = id,
            version = AggregateVersion(1),
            userId = userId,
            flightIdentity = flightIdentity
        )
    }
}

/**
 * 正常な監視状態。
 */
data class NormalTicket(
    override val id: TicketId,
    override val version: AggregateVersion,
    override val userId: UserId,
    override val flightIdentity: FlightIdentity,
) : Ticket()

/**
 * 異常を検知しており、ユーザーがまだその事実を確認していない状態。
 */
data class AlertTicket(
    override val id: TicketId,
    override val version: AggregateVersion,
    override val userId: UserId,
    override val flightIdentity: FlightIdentity,
    val currentAnomaly: Anomaly,
) : Ticket()

/**
 * 異常が発生しているが、ユーザーがその事実をすでに確認（承諾）済みの状態。
 */
data class AcknowledgedTicket(
    override val id: TicketId,
    override val version: AggregateVersion,
    override val userId: UserId,
    override val flightIdentity: FlightIdentity,
    val acknowledgedAnomaly: Anomaly,
) : Ticket()

/**
 * 監視が終了したチケット。
 */
data class FinishedTicket(
    override val id: TicketId,
    override val version: AggregateVersion,
    override val userId: UserId,
    override val flightIdentity: FlightIdentity,
    val reason: FinishReason,
) : Ticket()

/**
 * 監視が終了した理由。
 */
enum class FinishReason {
    ARRIVED,
    STOPPED,
    CANCELED_ACCEPTED
}
