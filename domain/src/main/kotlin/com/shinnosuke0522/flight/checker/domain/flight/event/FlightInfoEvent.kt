package com.shinnosuke0522.flight.checker.domain.flight.event

import com.shinnosuke0522.flight.checker.domain.base.event.DomainEvent
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.flight.model.ArrivedFlightInfo
import com.shinnosuke0522.flight.checker.domain.flight.model.CanceledFlightInfo
import com.shinnosuke0522.flight.checker.domain.flight.model.DelayedFlightInfo
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightInfo
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightPoint
import com.shinnosuke0522.flight.checker.domain.flight.model.ScheduledFlightInfo
import com.shinnosuke0522.flight.checker.domain.flight.model.UncertainFlightInfo
import com.shinnosuke0522.flight.checker.domain.shared.value.FlightIdentity
import java.time.Instant

/**
 * フライト情報（FlightInfo）に関するドメインイベントの基底インターフェース。
 */
sealed interface FlightInfoEvent : DomainEvent<FlightIdentity>

/**
 * 新しいフライト情報がシステムに登録されたことを表すイベント。
 */
data class FlightInfoRegistered(
    override val id: DomainEventId,
    override val aggregateId: FlightIdentity,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta,
    val departurePoint: FlightPoint,
    val arrivalPoint: FlightPoint,
    val scheduledDepartureTime: Instant,
    val scheduledArrivalTime: Instant
) : FlightInfoEvent

/**
 * フライトが遅延したことを表すイベント。
 */
data class FlightDelayed(
    override val id: DomainEventId,
    override val aggregateId: FlightIdentity,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta,
    val oldInfo: FlightInfo,
    val newInfo: DelayedFlightInfo
) : FlightInfoEvent

/**
 * フライトが欠航（キャンセル）されたことを表すイベント。
 */
data class FlightCanceled(
    override val id: DomainEventId,
    override val aggregateId: FlightIdentity,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta,
    val oldInfo: FlightInfo,
    val newInfo: CanceledFlightInfo
) : FlightInfoEvent

/**
 * フライトが目的地に到着したことを表すイベント。
 */
data class FlightArrived(
    override val id: DomainEventId,
    override val aggregateId: FlightIdentity,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta,
    val oldInfo: FlightInfo,
    val newInfo: ArrivedFlightInfo
) : FlightInfoEvent

/**
 * フライトの動静が不明（または欠航の疑いがある）になったことを表すイベント。
 */
data class FlightStatusUncertain(
    override val id: DomainEventId,
    override val aggregateId: FlightIdentity,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta,
    val oldInfo: FlightInfo,
    val newInfo: UncertainFlightInfo
) : FlightInfoEvent

/**
 * 遅延や動静不明だったフライトが、定刻（予定通り）の状態に復帰したことを表すイベント。
 */
data class FlightOnScheduleReturned(
    override val id: DomainEventId,
    override val aggregateId: FlightIdentity,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta,
    val oldInfo: FlightInfo,
    val newInfo: ScheduledFlightInfo
) : FlightInfoEvent

/**
 * 対象フライトの能動的な監視（Webhook登録等）が開始されたことを表すイベント。
 */
data class FlightMonitoringActivated(
    override val id: DomainEventId,
    override val aggregateId: FlightIdentity,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta
) : FlightInfoEvent

/**
 * 対象フライトの能動的な監視が正常に終了したことを表すイベント。
 */
data class FlightMonitoringCompleted(
    override val id: DomainEventId,
    override val aggregateId: FlightIdentity,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta
) : FlightInfoEvent

/**
 * 対象フライトの能動的な監視が失敗したことを表すイベント。
 */
data class FlightMonitoringFailed(
    override val id: DomainEventId,
    override val aggregateId: FlightIdentity,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta,
    val reason: String
) : FlightInfoEvent
