package com.shinnosuke0522.flight.checker.domain.flight.info.service

import arrow.core.Either
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.base.model.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.model.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfo
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfoError
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfoRegistered
import com.shinnosuke0522.flight.checker.domain.flight.info.model.SameFlightPointError
import com.shinnosuke0522.flight.checker.domain.flight.info.model.ScheduledArrivalTimeBeforeDepartureTimeError

object FlightInfoFactory {

    fun register(
        command: FlightInfoRegisterCommand
    ): Either<FlightInfoError, Pair<FlightInfo, FlightInfoRegistered>> = either {
        ensure(command.departurePoint != command.arrivalPoint) {
            SameFlightPointError
        }
        ensure(!command.scheduledArrivalTime.isBefore(command.scheduledDepartureTime)) {
            ScheduledArrivalTimeBeforeDepartureTimeError
        }

        val event = FlightInfoRegistered(
            id = DomainEventId.generate(),
            aggregateId = command.flightIdentity,
            sequenceNumber = AggregateVersion().nextVersion().value,
            meta = DomainEventMeta.forRootEvent { command.occurredAt },
            departurePoint = command.departurePoint,
            arrivalPoint = command.arrivalPoint,
            scheduledDepartureTime = command.scheduledDepartureTime,
            scheduledArrivalTime = command.scheduledArrivalTime
        )

        val flightInfo = FlightInfo.replay(nonEmptyListOf(event))
        Pair(flightInfo, event)
    }
}
