package com.shinnosuke0522.flight.checker.domain.flight.info.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.shinnosuke0522.flight.checker.domain.base.model.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.model.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.flight.info.model.ArrivedFlightInfo
import com.shinnosuke0522.flight.checker.domain.flight.info.model.CanceledFlightInfo
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightArrived
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightCanceled
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightDelayed
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfo
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfoAlreadyFinishedError
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfoAlreadyOnScheduleError
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfoBusinessRuleError
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfoEvent
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightOnScheduleReturned
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightStatusUncertain
import com.shinnosuke0522.flight.checker.domain.flight.info.model.ScheduledFlightInfo

object FlightInfoStatusUpdater {

    fun delay(
        flightInfo: FlightInfo,
        command: FlightInfoDelayCommand
    ): Either<FlightInfoBusinessRuleError, Pair<FlightInfo, FlightInfoEvent>> = either {
        ensure(flightInfo !is CanceledFlightInfo && flightInfo !is ArrivedFlightInfo) {
            FlightInfoAlreadyFinishedError(flightInfo.id)
        }

        val event = FlightDelayed(
            id = DomainEventId.generate(),
            aggregateId = flightInfo.id,
            sequenceNumber = flightInfo.version.nextVersion().value,
            meta = DomainEventMeta.forCausedEvent(
                clock = { command.occurredAt },
                correlationId = command.correlationId,
                causationId = command.causationId
            ),
            estimatedDepartureTime = command.estimatedDepartureTime,
            estimatedArrivalTime = command.estimatedArrivalTime
        )

        Pair(flightInfo.apply(event), event)
    }

    fun cancel(
        flightInfo: FlightInfo,
        command: FlightInfoCancelCommand
    ): Either<FlightInfoBusinessRuleError, Pair<FlightInfo, FlightInfoEvent>> = either {
        ensure(flightInfo !is CanceledFlightInfo && flightInfo !is ArrivedFlightInfo) {
            FlightInfoAlreadyFinishedError(flightInfo.id)
        }

        val event = FlightCanceled(
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

    fun arrive(
        flightInfo: FlightInfo,
        command: FlightInfoArriveCommand
    ): Either<FlightInfoBusinessRuleError, Pair<FlightInfo, FlightInfoEvent>> = either {
        ensure(flightInfo !is CanceledFlightInfo && flightInfo !is ArrivedFlightInfo) {
            FlightInfoAlreadyFinishedError(flightInfo.id)
        }

        val event = FlightArrived(
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

    fun markAsUncertain(
        flightInfo: FlightInfo,
        command: FlightInfoUncertainCommand
    ): Either<FlightInfoBusinessRuleError, Pair<FlightInfo, FlightInfoEvent>> = either {
        ensure(flightInfo !is CanceledFlightInfo && flightInfo !is ArrivedFlightInfo) {
            FlightInfoAlreadyFinishedError(flightInfo.id)
        }

        val event = FlightStatusUncertain(
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

    fun returnToSchedule(
        flightInfo: FlightInfo,
        command: FlightInfoReturnToScheduleCommand
    ): Either<FlightInfoBusinessRuleError, Pair<FlightInfo, FlightInfoEvent>> = either {
        ensure(flightInfo !is CanceledFlightInfo && flightInfo !is ArrivedFlightInfo) {
            FlightInfoAlreadyFinishedError(flightInfo.id)
        }
        ensure(flightInfo !is ScheduledFlightInfo) {
            FlightInfoAlreadyOnScheduleError(flightInfo.id)
        }

        val event = FlightOnScheduleReturned(
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
}
