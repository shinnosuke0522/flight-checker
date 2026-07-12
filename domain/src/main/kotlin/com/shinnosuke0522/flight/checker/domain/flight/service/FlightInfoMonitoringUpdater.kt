package com.shinnosuke0522.flight.checker.domain.flight.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.flight.error.FlightInfoBusinessRuleError
import com.shinnosuke0522.flight.checker.domain.flight.error.FlightMonitoringAlreadyActivatedError
import com.shinnosuke0522.flight.checker.domain.flight.error.FlightMonitoringNotActivatedError
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightInfoEvent
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightMonitoringActivated
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightMonitoringCompleted
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightMonitoringFailed
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightInfo
import com.shinnosuke0522.flight.checker.domain.flight.model.MonitoringStatus

object FlightInfoMonitoringUpdater {

    fun activateMonitoring(
        flightInfo: FlightInfo,
        command: FlightInfoActivateMonitoringCommand
    ): Either<FlightInfoBusinessRuleError, Pair<FlightInfo, FlightInfoEvent>> = either {
        ensure(flightInfo.monitoringStatus == MonitoringStatus.IDLE) {
            FlightMonitoringAlreadyActivatedError(flightInfo.id)
        }

        val event = FlightMonitoringActivated(
            id = DomainEventId.generate(),
            aggregateId = flightInfo.id,
            sequenceNumber = flightInfo.version.nextVersion().value,
            meta = DomainEventMeta.forCausedEvent(
                clock = { command.occurredAt },
                correlationId = command.correlationId,
                causationId = command.causationId
            )
        )

        Pair(flightInfo.apply(event), event)
    }

    fun completeMonitoring(
        flightInfo: FlightInfo,
        command: FlightInfoCompleteMonitoringCommand
    ): Either<FlightInfoBusinessRuleError, Pair<FlightInfo, FlightInfoEvent>> = either {
        ensure(flightInfo.monitoringStatus == MonitoringStatus.ACTIVATED) {
            FlightMonitoringNotActivatedError(flightInfo.id)
        }

        val event = FlightMonitoringCompleted(
            id = DomainEventId.generate(),
            aggregateId = flightInfo.id,
            sequenceNumber = flightInfo.version.nextVersion().value,
            meta = DomainEventMeta.forCausedEvent(
                clock = { command.occurredAt },
                correlationId = command.correlationId,
                causationId = command.causationId
            )
        )

        Pair(flightInfo.apply(event), event)
    }

    fun failMonitoring(
        flightInfo: FlightInfo,
        command: FlightInfoFailMonitoringCommand
    ): Either<FlightInfoBusinessRuleError, Pair<FlightInfo, FlightInfoEvent>> = either {
        ensure(flightInfo.monitoringStatus == MonitoringStatus.ACTIVATED) {
            FlightMonitoringNotActivatedError(flightInfo.id)
        }

        val event = FlightMonitoringFailed(
            id = DomainEventId.generate(),
            aggregateId = flightInfo.id,
            sequenceNumber = flightInfo.version.nextVersion().value,
            meta = DomainEventMeta.forCausedEvent(
                clock = { command.occurredAt },
                correlationId = command.correlationId,
                causationId = command.causationId
            ),
            reason = command.reason
        )

        Pair(flightInfo.apply(event), event)
    }
}
