package com.shinnosuke0522.flight.checker.domain.flight.model

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.base.model.EventSourcingAggregateRoot
import com.shinnosuke0522.flight.checker.domain.flight.error.FlightInfoValidationError
import com.shinnosuke0522.flight.checker.domain.flight.error.SameFlightPointError
import com.shinnosuke0522.flight.checker.domain.flight.error.ScheduledArrivalTimeBeforeDepartureTimeError
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightArrived
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightCanceled
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightDelayed
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightInfoEvent
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightInfoRegistered
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightMonitoringActivated
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightMonitoringCompleted
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightMonitoringFailed
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightOnScheduleReturned
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightStatusUncertain
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import java.time.Instant

/**
 * 特定のフライト便の状況（事実）を表すモデル。
 *
 * 主にフライトの監視・追跡の要否を判断し、ユーザーにトラブルを通知するための情報を保持する。
 */
sealed class FlightInfo : EventSourcingAggregateRoot<FlightIdentity, FlightInfoEvent, FlightInfo> {
    abstract val departurePoint: FlightPoint
    abstract val arrivalPoint: FlightPoint
    abstract val scheduledDepartureTime: Instant
    abstract val scheduledArrivalTime: Instant
    abstract val monitoringStatus: MonitoringStatus

    override fun apply(event: FlightInfoEvent): FlightInfo = when (event) {
        is FlightInfoRegistered -> throw IllegalStateException("FlightInfoRegistered must be first event")

        is FlightDelayed -> DelayedFlightInfo(
            flightIdentity = id,
            version = AggregateVersion(event.sequenceNumber),
            departurePoint = departurePoint,
            arrivalPoint = arrivalPoint,
            scheduledDepartureTime = scheduledDepartureTime,
            scheduledArrivalTime = scheduledArrivalTime,
            estimatedDepartureTime = event.estimatedDepartureTime,
            estimatedArrivalTime = event.estimatedArrivalTime,
        )

        is FlightCanceled -> CanceledFlightInfo(
            id = id,
            version = AggregateVersion(event.sequenceNumber),
            departurePoint = departurePoint,
            arrivalPoint = arrivalPoint,
            scheduledDepartureTime = scheduledDepartureTime,
            scheduledArrivalTime = scheduledArrivalTime,
        )

        is FlightArrived -> ArrivedFlightInfo(
            flightIdentity = id,
            version = AggregateVersion(event.sequenceNumber),
            departurePoint = departurePoint,
            arrivalPoint = arrivalPoint,
            scheduledDepartureTime = scheduledDepartureTime,
            scheduledArrivalTime = scheduledArrivalTime,
        )

        is FlightStatusUncertain -> UncertainFlightInfo(
            id = id,
            version = AggregateVersion(event.sequenceNumber),
            departurePoint = departurePoint,
            arrivalPoint = arrivalPoint,
            scheduledDepartureTime = scheduledDepartureTime,
            scheduledArrivalTime = scheduledArrivalTime,
            reason = event.reason,
        )

        is FlightOnScheduleReturned -> ScheduledFlightInfo(
            id = id,
            departurePoint = departurePoint,
            arrivalPoint = arrivalPoint,
            scheduledDepartureTime = scheduledDepartureTime,
            scheduledArrivalTime = scheduledArrivalTime,
            version = AggregateVersion(event.sequenceNumber),
            monitoringStatus = monitoringStatus
        ).fold({ error -> throw IllegalStateException(error.toString()) }, { it })

        is FlightMonitoringActivated -> updateMonitoringStatus(
            MonitoringStatus.ACTIVATED,
            AggregateVersion(event.sequenceNumber)
        )

        is FlightMonitoringCompleted -> updateMonitoringStatus(
            MonitoringStatus.COMPLETED,
            AggregateVersion(event.sequenceNumber)
        )

        is FlightMonitoringFailed -> updateMonitoringStatus(
            MonitoringStatus.FAILED,
            AggregateVersion(event.sequenceNumber)
        )
    }

    protected abstract fun updateMonitoringStatus(status: MonitoringStatus, version: AggregateVersion): FlightInfo

    companion object {
        fun replay(events: NonEmptyList<FlightInfoEvent>): FlightInfo {
            val firstEvent = events.head
            require(firstEvent is FlightInfoRegistered) {
                "Replay must start with FlightInfoRegistered event, but was ${firstEvent::class.simpleName}"
            }

            val initialInfo = from(firstEvent)

            return events.tail.fold(initialInfo) { info, event ->
                info.apply(event)
            }
        }

        private fun from(event: FlightInfoRegistered): FlightInfo = ScheduledFlightInfo(
            id = event.aggregateId,
            departurePoint = event.departurePoint,
            arrivalPoint = event.arrivalPoint,
            scheduledDepartureTime = event.scheduledDepartureTime,
            scheduledArrivalTime = event.scheduledArrivalTime,
            version = AggregateVersion(event.sequenceNumber)
        ).fold({ error -> throw IllegalStateException(error.toString()) }, { it })
    }
}

/**
 * 定刻通りのフライト情報。
 */
@ConsistentCopyVisibility
data class ScheduledFlightInfo private constructor(
    override val id: FlightIdentity,
    override val version: AggregateVersion,
    override val departurePoint: FlightPoint,
    override val arrivalPoint: FlightPoint,
    override val scheduledDepartureTime: Instant,
    override val scheduledArrivalTime: Instant,
    override val monitoringStatus: MonitoringStatus = MonitoringStatus.IDLE,
) : FlightInfo() {
    override fun updateMonitoringStatus(status: MonitoringStatus, version: AggregateVersion): FlightInfo =
        Companion(
            id = id,
            version = version,
            departurePoint = departurePoint,
            arrivalPoint = arrivalPoint,
            scheduledDepartureTime = scheduledDepartureTime,
            scheduledArrivalTime = scheduledArrivalTime,
            monitoringStatus = status
        ).fold({ error -> throw IllegalStateException(error.toString()) }, { it })

    companion object {
        operator fun invoke(
            id: FlightIdentity,
            version: AggregateVersion,
            departurePoint: FlightPoint,
            arrivalPoint: FlightPoint,
            scheduledDepartureTime: Instant,
            scheduledArrivalTime: Instant,
            monitoringStatus: MonitoringStatus = MonitoringStatus.IDLE,
        ): Either<NonEmptyList<FlightInfoValidationError>, ScheduledFlightInfo> = either {
            ensure(scheduledDepartureTime != scheduledArrivalTime) {
                nonEmptyListOf(SameFlightPointError)
            }
            ensure(scheduledDepartureTime.isBefore(scheduledArrivalTime)) {
                nonEmptyListOf(ScheduledArrivalTimeBeforeDepartureTimeError)
            }
            ScheduledFlightInfo(
                id = id,
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
    override val version: AggregateVersion,
    override val departurePoint: FlightPoint,
    override val arrivalPoint: FlightPoint,
    override val scheduledDepartureTime: Instant,
    override val scheduledArrivalTime: Instant,
    val estimatedDepartureTime: Instant?,
    val estimatedArrivalTime: Instant?,
    override val monitoringStatus: MonitoringStatus = MonitoringStatus.ACTIVATED,
) : FlightInfo() {
    override fun updateMonitoringStatus(status: MonitoringStatus, version: AggregateVersion): FlightInfo =
        Companion(
            flightIdentity = id,
            version = version,
            departurePoint = departurePoint,
            arrivalPoint = arrivalPoint,
            scheduledDepartureTime = scheduledDepartureTime,
            scheduledArrivalTime = scheduledArrivalTime,
            estimatedDepartureTime = estimatedDepartureTime,
            estimatedArrivalTime = estimatedArrivalTime,
            monitoringStatus = status
        )

    companion object {
        operator fun invoke(
            flightIdentity: FlightIdentity,
            version: AggregateVersion,
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
            estimatedArrivalTime = estimatedArrivalTime,
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
    override val version: AggregateVersion,
    override val departurePoint: FlightPoint,
    override val arrivalPoint: FlightPoint,
    override val scheduledDepartureTime: Instant,
    override val scheduledArrivalTime: Instant,
    override val monitoringStatus: MonitoringStatus = MonitoringStatus.COMPLETED,
) : FlightInfo() {
    override fun updateMonitoringStatus(status: MonitoringStatus, version: AggregateVersion): FlightInfo =
        Companion(
            flightIdentity = id,
            version = version,
            departurePoint = departurePoint,
            arrivalPoint = arrivalPoint,
            scheduledDepartureTime = scheduledDepartureTime,
            scheduledArrivalTime = scheduledArrivalTime,
            monitoringStatus = status
        )

    companion object {
        operator fun invoke(
            flightIdentity: FlightIdentity,
            version: AggregateVersion,
            departurePoint: FlightPoint,
            arrivalPoint: FlightPoint,
            scheduledDepartureTime: Instant,
            scheduledArrivalTime: Instant,
            monitoringStatus: MonitoringStatus = MonitoringStatus.COMPLETED,
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
    override val version: AggregateVersion,
    override val departurePoint: FlightPoint,
    override val arrivalPoint: FlightPoint,
    override val scheduledDepartureTime: Instant,
    override val scheduledArrivalTime: Instant,
    override val monitoringStatus: MonitoringStatus = MonitoringStatus.COMPLETED,
) : FlightInfo() {
    override fun updateMonitoringStatus(status: MonitoringStatus, version: AggregateVersion): FlightInfo =
        Companion(
            id = id,
            version = version,
            departurePoint = departurePoint,
            arrivalPoint = arrivalPoint,
            scheduledDepartureTime = scheduledDepartureTime,
            scheduledArrivalTime = scheduledArrivalTime,
            monitoringStatus = status
        )

    companion object {
        operator fun invoke(
            id: FlightIdentity,
            version: AggregateVersion,
            departurePoint: FlightPoint,
            arrivalPoint: FlightPoint,
            scheduledDepartureTime: Instant,
            scheduledArrivalTime: Instant,
            monitoringStatus: MonitoringStatus = MonitoringStatus.COMPLETED,
        ) = CanceledFlightInfo(
            id = id,
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
    override val version: AggregateVersion,
    override val departurePoint: FlightPoint,
    override val arrivalPoint: FlightPoint,
    override val scheduledDepartureTime: Instant,
    override val scheduledArrivalTime: Instant,
    val reason: String,
    override val monitoringStatus: MonitoringStatus = MonitoringStatus.ACTIVATED,
) : FlightInfo() {
    override fun updateMonitoringStatus(status: MonitoringStatus, version: AggregateVersion): FlightInfo =
        Companion(
            id = id,
            version = version,
            departurePoint = departurePoint,
            arrivalPoint = arrivalPoint,
            scheduledDepartureTime = scheduledDepartureTime,
            scheduledArrivalTime = scheduledArrivalTime,
            reason = reason,
            monitoringStatus = status
        )

    companion object {
        operator fun invoke(
            id: FlightIdentity,
            version: AggregateVersion,
            departurePoint: FlightPoint,
            arrivalPoint: FlightPoint,
            scheduledDepartureTime: Instant,
            scheduledArrivalTime: Instant,
            reason: String,
            monitoringStatus: MonitoringStatus = MonitoringStatus.ACTIVATED,
        ) = UncertainFlightInfo(
            id = id,
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
