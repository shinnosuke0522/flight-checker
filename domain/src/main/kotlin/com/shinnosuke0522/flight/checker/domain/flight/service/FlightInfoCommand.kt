package com.shinnosuke0522.flight.checker.domain.flight.service

import com.shinnosuke0522.flight.checker.domain.base.event.CorrelationId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightPoint
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import java.time.Instant

sealed interface FlightInfoCommand {
    val occurredAt: Instant
    val correlationId: CorrelationId
}

sealed interface FlightInfoCausedCommand : FlightInfoCommand {
    val causationId: DomainEventId
}

data class FlightInfoRegisterCommand(
    override val occurredAt: Instant,
    override val correlationId: CorrelationId,
    val flightIdentity: FlightIdentity,
    val departurePoint: FlightPoint,
    val arrivalPoint: FlightPoint,
    val scheduledDepartureTime: Instant,
    val scheduledArrivalTime: Instant
) : FlightInfoCommand

data class FlightInfoDelayCommand(
    override val occurredAt: Instant,
    override val correlationId: CorrelationId,
    override val causationId: DomainEventId,
    val estimatedDepartureTime: Instant?,
    val estimatedArrivalTime: Instant?
) : FlightInfoCausedCommand

data class FlightInfoCancelCommand(
    override val occurredAt: Instant,
    override val correlationId: CorrelationId,
    override val causationId: DomainEventId
) : FlightInfoCausedCommand

data class FlightInfoArriveCommand(
    override val occurredAt: Instant,
    override val correlationId: CorrelationId,
    override val causationId: DomainEventId,
    val actualArrivalTime: Instant
) : FlightInfoCausedCommand

data class FlightInfoUncertainCommand(
    override val occurredAt: Instant,
    override val correlationId: CorrelationId,
    override val causationId: DomainEventId,
    val reason: String
) : FlightInfoCausedCommand

data class FlightInfoReturnToScheduleCommand(
    override val occurredAt: Instant,
    override val correlationId: CorrelationId,
    override val causationId: DomainEventId
) : FlightInfoCausedCommand

data class FlightInfoActivateMonitoringCommand(
    override val occurredAt: Instant,
    override val correlationId: CorrelationId,
    override val causationId: DomainEventId
) : FlightInfoCausedCommand

data class FlightInfoCompleteMonitoringCommand(
    override val occurredAt: Instant,
    override val correlationId: CorrelationId,
    override val causationId: DomainEventId
) : FlightInfoCausedCommand

data class FlightInfoFailMonitoringCommand(
    override val occurredAt: Instant,
    override val correlationId: CorrelationId,
    override val causationId: DomainEventId,
    val reason: String
) : FlightInfoCausedCommand
