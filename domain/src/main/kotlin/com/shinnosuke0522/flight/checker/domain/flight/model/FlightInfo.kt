package com.shinnosuke0522.flight.checker.domain.flight.model

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateRoot
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.shared.value.FlightIdentity
import java.time.Instant

/**
 * 特定のフライト便の状況（事実）を表すモデル。
 *
 * 主にフライトの監視・追跡の要否を判断し、ユーザーにトラブルを通知するための情報を保持する。
 */
sealed interface FlightInfo : AggregateRoot<FlightIdentity> {
    val departurePoint: FlightPoint
    val arrivalPoint: FlightPoint
    val scheduledDepartureTime: Instant
    val scheduledArrivalTime: Instant
    val monitoringStatus: MonitoringStatus
}

/**
 * 定刻通りのフライト情報。
 */
@ConsistentCopyVisibility
data class ScheduledFlightInfo private constructor(
    override val id: FlightIdentity,
    override val version: AggregateVersion = AggregateVersion(),
    override val departurePoint: FlightPoint,
    override val arrivalPoint: FlightPoint,
    override val scheduledDepartureTime: Instant,
    override val scheduledArrivalTime: Instant,
    override val monitoringStatus: MonitoringStatus = MonitoringStatus.IDLE,
) : FlightInfo {
    companion object {
        operator fun invoke(
            flightIdentity: FlightIdentity,
            departurePoint: FlightPoint,
            arrivalPoint: FlightPoint,
            scheduledDepartureTime: Instant,
            scheduledArrivalTime: Instant,
            monitoringStatus: MonitoringStatus = MonitoringStatus.IDLE,
            version: AggregateVersion = AggregateVersion(),
        ): Either<NonEmptyList<FlightInfoValidationError>, ScheduledFlightInfo> = either {
            ensure(scheduledDepartureTime != scheduledArrivalTime) {
                nonEmptyListOf(SameFlightPointError)
            }
            ensure(scheduledDepartureTime.isBefore(scheduledArrivalTime)) {
                nonEmptyListOf(ScheduledArrivalTimeBeforeDepartureTimeError)
            }
            ScheduledFlightInfo(
                id = flightIdentity,
                departurePoint = departurePoint,
                arrivalPoint = arrivalPoint,
                scheduledDepartureTime = scheduledDepartureTime,
                scheduledArrivalTime = scheduledArrivalTime,
                monitoringStatus = monitoringStatus,
                version = version
            )
        }
    }
}

/**
 * 遅延が発生しているフライト情報。
 */
@ConsistentCopyVisibility
data class DelayedFlightInfo private constructor(
    override val id: FlightIdentity,
    override val version: AggregateVersion = AggregateVersion(),
    override val departurePoint: FlightPoint,
    override val arrivalPoint: FlightPoint,
    override val scheduledDepartureTime: Instant,
    override val scheduledArrivalTime: Instant,
    val estimatedDepartureTime: Instant?,
    val estimatedArrivalTime: Instant?,
    override val monitoringStatus: MonitoringStatus = MonitoringStatus.ACTIVATED,
) : FlightInfo {
    companion object {
        operator fun invoke(
            flightIdentity: FlightIdentity,
            version: AggregateVersion = AggregateVersion(),
            departurePoint: FlightPoint,
            arrivalPoint: FlightPoint,
            scheduledDepartureTime: Instant,
            scheduledArrivalTime: Instant,
            estimatedDepartureTime: Instant? = null,
            estimatedArrivalTime: Instant? = null,
            monitoringStatus: MonitoringStatus = MonitoringStatus.ACTIVATED,
        ) = DelayedFlightInfo(
            id = flightIdentity,
            version = version,
            departurePoint = departurePoint,
            arrivalPoint = arrivalPoint,
            scheduledDepartureTime = scheduledDepartureTime,
            scheduledArrivalTime = scheduledArrivalTime,
            estimatedDepartureTime = estimatedDepartureTime,
            estimatedArrivalTime =  estimatedArrivalTime,
            monitoringStatus = monitoringStatus,
        )
    }
}

/**
 * 目的地に到着済みのフライト情報。
 *
 * この状態に遷移した時点で、該当フライトの監視役割は終了する。
 */
@ConsistentCopyVisibility
data class ArrivedFlightInfo private constructor(
    override val id: FlightIdentity,
    override val version: AggregateVersion = AggregateVersion(),
    override val departurePoint: FlightPoint,
    override val arrivalPoint: FlightPoint,
    override val scheduledDepartureTime: Instant,
    override val scheduledArrivalTime: Instant,
    override val monitoringStatus: MonitoringStatus = MonitoringStatus.COMPLETED,
) : FlightInfo {
    companion object {
        operator fun invoke(
            flightIdentity: FlightIdentity,
            departurePoint: FlightPoint,
            arrivalPoint: FlightPoint,
            scheduledDepartureTime: Instant,
            scheduledArrivalTime: Instant,
            monitoringStatus: MonitoringStatus = MonitoringStatus.COMPLETED,
            version: AggregateVersion = AggregateVersion(),
        ) = ArrivedFlightInfo(
            id = flightIdentity,
            version = version,
            departurePoint = departurePoint,
            arrivalPoint = arrivalPoint,
            scheduledDepartureTime = scheduledDepartureTime,
            scheduledArrivalTime = scheduledArrivalTime,
            monitoringStatus = monitoringStatus,
        )
    }
}

/**
 * 欠航が確定したフライト情報。
 *
 * この状態に遷移した時点で監視は終了するが、関連する旅行（Travel）では対応が必要となる。
 */
@ConsistentCopyVisibility
data class CanceledFlightInfo private constructor(
    override val id: FlightIdentity,
    override val version: AggregateVersion = AggregateVersion(),
    override val departurePoint: FlightPoint,
    override val arrivalPoint: FlightPoint,
    override val scheduledDepartureTime: Instant,
    override val scheduledArrivalTime: Instant,
    override val monitoringStatus: MonitoringStatus = MonitoringStatus.COMPLETED,
) : FlightInfo {
    companion object {
        operator fun invoke(
            flightIdentity: FlightIdentity,
            departurePoint: FlightPoint,
            arrivalPoint: FlightPoint,
            scheduledDepartureTime: Instant,
            scheduledArrivalTime: Instant,
            monitoringStatus: MonitoringStatus = MonitoringStatus.COMPLETED,
            version: AggregateVersion = AggregateVersion(),
        ) = CanceledFlightInfo(
            id = flightIdentity,
            version = version,
            departurePoint = departurePoint,
            arrivalPoint = arrivalPoint,
            scheduledDepartureTime = scheduledDepartureTime,
            scheduledArrivalTime = scheduledArrivalTime,
            monitoringStatus = monitoringStatus,
        )
    }
}

/**
 * 状況が不確実な（動静不明、または欠航の疑いがある）フライト情報。
 *
 * 予定時刻を過ぎてもAPIの更新がない場合などに、システムによる推論の結果として扱われる。
 */
@ConsistentCopyVisibility
data class UncertainFlightInfo private constructor(
    override val id: FlightIdentity,
    override val version: AggregateVersion = AggregateVersion(),
    override val departurePoint: FlightPoint,
    override val arrivalPoint: FlightPoint,
    override val scheduledDepartureTime: Instant,
    override val scheduledArrivalTime: Instant,
    val reason: String,
    override val monitoringStatus: MonitoringStatus = MonitoringStatus.ACTIVATED,
) : FlightInfo {
    companion object {
        operator fun invoke(
            flightIdentity: FlightIdentity,
            departurePoint: FlightPoint,
            arrivalPoint: FlightPoint,
            scheduledDepartureTime: Instant,
            scheduledArrivalTime: Instant,
            reason: String,
            monitoringStatus: MonitoringStatus = MonitoringStatus.ACTIVATED,
            version: AggregateVersion = AggregateVersion(),
        ) = UncertainFlightInfo(
            id = flightIdentity,
            version = version,
            departurePoint = departurePoint,
            arrivalPoint = arrivalPoint,
            scheduledDepartureTime = scheduledDepartureTime,
            scheduledArrivalTime = scheduledArrivalTime,
            reason = reason,
            monitoringStatus = monitoringStatus,
        )
    }
}
