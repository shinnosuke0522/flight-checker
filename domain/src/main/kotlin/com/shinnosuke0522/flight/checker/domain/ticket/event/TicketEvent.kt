package com.shinnosuke0522.flight.checker.domain.ticket.event

import com.shinnosuke0522.flight.checker.domain.base.event.DomainEvent
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.ticket.model.Anomaly
import com.shinnosuke0522.flight.checker.domain.ticket.model.AnomalyDelayed
import com.shinnosuke0522.flight.checker.domain.ticket.model.AnomalyUncertain
import com.shinnosuke0522.flight.checker.domain.ticket.model.FinishReason
import com.shinnosuke0522.flight.checker.domain.ticket.model.TicketId
import com.shinnosuke0522.flight.checker.domain.ticket.model.UserId

/**
 * チケット（Ticket）に関するドメインイベントの基底インターフェース。
 */
sealed interface TicketEvent : DomainEvent<TicketId>

/**
 * チケットが監視対象として登録されたことを表すイベント。
 */
data class TicketRegistered(
    override val id: DomainEventId,
    override val aggregateId: TicketId,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta,
    val userId: UserId,
    val flightIdentity: FlightIdentity,
) : TicketEvent

/**
 * フライトの遅延を検知したことを表すイベント。
 */
data class TicketFlightDelayed(
    override val id: DomainEventId,
    override val aggregateId: TicketId,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta,
    val detail: AnomalyDelayed,
) : TicketEvent

/**
 * フライトの欠航を検知したことを表すイベント。
 */
data class TicketFlightCanceled(
    override val id: DomainEventId,
    override val aggregateId: TicketId,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta,
) : TicketEvent

/**
 * フライトの動静が不確実であることを検知したことを表すイベント。
 */
data class TicketFlightUncertain(
    override val id: DomainEventId,
    override val aggregateId: TicketId,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta,
    val detail: AnomalyUncertain,
) : TicketEvent

/**
 * 検知されていた異常が解消され、正常（Scheduled）に戻ったことを表すイベント。
 */
data class TicketAnomalyRecovered(
    override val id: DomainEventId,
    override val aggregateId: TicketId,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta,
) : TicketEvent

/**
 * ユーザーが現在の異常状態を認識・確認したことを表すイベント。
 */
data class TicketAnomalyAcknowledged(
    override val id: DomainEventId,
    override val aggregateId: TicketId,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta,
    val acknowledgedAnomaly: Anomaly,
) : TicketEvent

/**
 * チケットの監視が終了したことを表すイベント。
 */
data class TicketFinished(
    override val id: DomainEventId,
    override val aggregateId: TicketId,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta,
    val reason: FinishReason,
) : TicketEvent
