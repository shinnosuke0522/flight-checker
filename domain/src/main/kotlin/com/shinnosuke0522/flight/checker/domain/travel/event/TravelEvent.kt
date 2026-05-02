package com.shinnosuke0522.flight.checker.domain.travel.event

import com.shinnosuke0522.flight.checker.domain.base.event.DomainEvent
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.shared.value.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.travel.model.Flights
import com.shinnosuke0522.flight.checker.domain.travel.model.Schedule
import com.shinnosuke0522.flight.checker.domain.travel.model.TravelId
import com.shinnosuke0522.flight.checker.domain.travel.model.TravelName

/**
 * 旅行（Travel）に関するドメインイベントの基底インターフェース。
 */
sealed interface TravelEvent : DomainEvent<TravelId>

/**
 * 新しい旅行が計画されたことを表すイベント。
 *
 * 旅行の初期作成時に発行される。
 */
data class TravelPlanned(
    override val id: DomainEventId,
    override val aggregateId: TravelId,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta,
    val name: TravelName,
    val schedule: Schedule,
    val flights: Flights
) : TravelEvent

/**
 * 旅行が開始されたことを表すイベント。
 *
 * 旅行の出発日（初日のフライトなど）に達した際に発行される。
 * [FlightSegmentMonitoringActivated] とは異なり、純粋に旅行期間に入ったことを意味する。
 */
data class TravelStarted(
    override val id: DomainEventId,
    override val aggregateId: TravelId,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta
) : TravelEvent

/**
 * 特定のフライトセグメントの監視を有効化すべきタイミングに達したことを表すイベント。
 *
 * 旅行の出発が近づき、システムが対象フライト（FlightInfo）の能動的な監視
 *（外部APIへのWebhook登録など）を開始すべきタイミングで発行される。
 */
data class FlightSegmentMonitoringActivated(
    override val id: DomainEventId,
    override val aggregateId: TravelId,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta,
    val flightIdentity: FlightIdentity
) : TravelEvent

/**
 * フライトセグメントにトラブル（欠航・遅延等）が発生したことを表すイベント。
 *
 * 外部のフライト情報が更新され、旅程に含まれるフライトに影響が出た際に発行される。
 */
data class FlightSegmentDisrupted(
    override val id: DomainEventId,
    override val aggregateId: TravelId,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta,
    val flightIdentity: FlightIdentity,
    val reason: String
) : TravelEvent

/**
 * フライトセグメントの変更（振り替え等）が必要になったことを表すイベント。
 *
 * トラブルの発生を受け、ユーザーによる対応が必要であると判断された際に発行される。
 */
data class FlightSegmentChangeRequired(
    override val id: DomainEventId,
    override val aggregateId: TravelId,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta,
    val flightIdentity: FlightIdentity
) : TravelEvent

/**
 * 旅行のスケジュール（期間）が変更されたことを表すイベント。
 *
 * フライトの欠航による延泊や、ユーザーによる滞在期間の変更など、
 * 旅行全体の出発日または帰国日が更新された際に発行される。
 */
data class TravelScheduleChanged(
    override val id: DomainEventId,
    override val aggregateId: TravelId,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta,
    val newSchedule: Schedule
) : TravelEvent

/**
 * 新しいフライトセグメントが旅程に追加されたことを表すイベント。
 *
 * 既存の旅程への追加、または航空券の振り替え時に新しい便が確定した際に発行される。
 * 後続プロセスはこのイベントを受けて、該当フライトの監視を開始する。
 */
data class FlightSegmentAdded(
    override val id: DomainEventId,
    override val aggregateId: TravelId,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta,
    val flightIdentity: FlightIdentity
) : TravelEvent

/**
 * 既存のフライトセグメントが旅程から削除されたことを表すイベント。
 *
 * フライトの欠航による旅程からの除外、または航空券の振り替え時に旧い便が
 * 不要になった際に発行される。
 * 後続プロセスはこのイベントを受けて、該当フライトの監視（Webhook等）を解除する。
 */
data class FlightSegmentRemoved(
    override val id: DomainEventId,
    override val aggregateId: TravelId,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta,
    val flightIdentity: FlightIdentity
) : TravelEvent

/**
 * 旅行が予定通り完了したことを表すイベント。
 *
 * 全ての旅程を終えて帰国した際に発行される。
 * 後続プロセスはこのイベントを受けて、該当する旅行に関する全ての監視を停止する。
 */
data class TravelCompleted(
    override val id: DomainEventId,
    override val aggregateId: TravelId,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta
) : TravelEvent

/**
 * 旅行自体が中止（キャンセル）されたことを表すイベント。
 *
 * ユーザーの意思、あるいは不可抗力により旅行が継続不可能になった際に発行される。
 * 後続プロセスはこのイベントを受けて、該当する旅行に関する全ての監視を直ちに停止する。
 */
data class TravelCanceled(
    override val id: DomainEventId,
    override val aggregateId: TravelId,
    override val sequenceNumber: Long,
    override val meta: DomainEventMeta
) : TravelEvent
