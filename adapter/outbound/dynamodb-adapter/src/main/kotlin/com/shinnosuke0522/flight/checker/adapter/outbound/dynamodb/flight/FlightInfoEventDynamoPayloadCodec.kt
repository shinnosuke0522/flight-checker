package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.flight

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.eventstore.EventStoreItem
import com.shinnosuke0522.flight.checker.domain.base.event.CorrelationId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
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
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightPoint
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import org.springframework.stereotype.Component
import java.time.Instant

@Component
final class FlightInfoEventDynamoPayloadCodec(
    private val objectMapper: ObjectMapper
) {
    fun serialize(): (FlightInfoEvent) -> String = { event ->
        objectMapper.writeValueAsString(event.toDto())
    }

    fun deserialize(): (EventStoreItem) -> FlightInfoEvent = { item ->
        objectMapper.readValue<FlightInfoEventDynamoPayload>(item.payload)
            .toDomain(item.aggregateId, item.sequenceNumber)
    }
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventType"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = FlightInfoRegisteredDynamoPayload::class, name = "FlightInfoRegistered"),
    JsonSubTypes.Type(value = FlightDelayedDto::class, name = "FlightDelayed"),
    JsonSubTypes.Type(value = FlightCanceledDto::class, name = "FlightCanceled"),
    JsonSubTypes.Type(value = FlightArrivedDto::class, name = "FlightArrived"),
    JsonSubTypes.Type(value = FlightStatusUncertainDto::class, name = "FlightStatusUncertain"),
    JsonSubTypes.Type(value = FlightOnScheduleReturnedDto::class, name = "FlightOnScheduleReturned"),
    JsonSubTypes.Type(value = FlightMonitoringActivatedDto::class, name = "FlightMonitoringActivated"),
    JsonSubTypes.Type(value = FlightMonitoringCompletedDto::class, name = "FlightMonitoringCompleted"),
    JsonSubTypes.Type(value = FlightMonitoringFailedDto::class, name = "FlightMonitoringFailed")
)
sealed interface FlightInfoEventDynamoPayload {
    val id: String
    val occurredAt: String
    val correlationId: String
    val causationId: String?
}

data class FlightInfoRegisteredDynamoPayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?,
    val departureCountryCode: String,
    val departureAirportCode: String,
    val departureZoneId: String,
    val arrivalCountryCode: String,
    val arrivalAirportCode: String,
    val arrivalZoneId: String,
    val scheduledDepartureTime: String,
    val scheduledArrivalTime: String
) : FlightInfoEventDynamoPayload

data class FlightDelayedDto(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?,
    val estimatedDepartureTime: String?,
    val estimatedArrivalTime: String?
) : FlightInfoEventDynamoPayload

data class FlightCanceledDto(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?
) : FlightInfoEventDynamoPayload

data class FlightArrivedDto(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?
) : FlightInfoEventDynamoPayload

data class FlightStatusUncertainDto(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?,
    val reason: String
) : FlightInfoEventDynamoPayload

data class FlightOnScheduleReturnedDto(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?
) : FlightInfoEventDynamoPayload

data class FlightMonitoringActivatedDto(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?
) : FlightInfoEventDynamoPayload

data class FlightMonitoringCompletedDto(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?
) : FlightInfoEventDynamoPayload

data class FlightMonitoringFailedDto(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?,
    val reason: String
) : FlightInfoEventDynamoPayload

fun FlightInfoEvent.toDto(): FlightInfoEventDynamoPayload = when (this) {
    is FlightInfoRegistered -> FlightInfoRegisteredDynamoPayload(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value(),
        departureCountryCode = this.departurePoint.countryCode.value,
        departureAirportCode = this.departurePoint.airportCode.value,
        departureZoneId = this.departurePoint.zoneId.id,
        arrivalCountryCode = this.arrivalPoint.countryCode.value,
        arrivalAirportCode = this.arrivalPoint.airportCode.value,
        arrivalZoneId = this.arrivalPoint.zoneId.id,
        scheduledDepartureTime = this.scheduledDepartureTime.toString(),
        scheduledArrivalTime = this.scheduledArrivalTime.toString()
    )
    is FlightDelayed -> FlightDelayedDto(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value(),
        estimatedDepartureTime = this.estimatedDepartureTime?.toString(),
        estimatedArrivalTime = this.estimatedArrivalTime?.toString()
    )
    is FlightCanceled -> FlightCanceledDto(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value()
    )
    is FlightArrived -> FlightArrivedDto(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value()
    )
    is FlightStatusUncertain -> FlightStatusUncertainDto(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value(),
        reason = this.reason
    )
    is FlightOnScheduleReturned -> FlightOnScheduleReturnedDto(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value()
    )
    is FlightMonitoringActivated -> FlightMonitoringActivatedDto(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value()
    )
    is FlightMonitoringCompleted -> FlightMonitoringCompletedDto(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value()
    )
    is FlightMonitoringFailed -> FlightMonitoringFailedDto(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value(),
        reason = this.reason
    )
}

@Suppress("CyclomaticComplexMethod")
fun FlightInfoEventDynamoPayload.toDomain(aggregateIdStr: String, sequenceNumber: Long): FlightInfoEvent {
    val domainEventId = DomainEventId.invoke(this.id).getOrNull() ?: error("Invalid id")
    val aggregateId = FlightIdentity.fromString(aggregateIdStr).getOrNull() ?: error("Invalid aggregateId")
    val meta = DomainEventMeta(
        occurredAt = Instant.parse(this.occurredAt),
        correlationId = CorrelationId.invoke(this.correlationId).getOrNull() ?: error("Invalid correlationId"),
        causationId = this.causationId?.let { DomainEventId.invoke(it).getOrNull() ?: error("Invalid causationId") }
    )

    return when (this) {
        is FlightInfoRegisteredDynamoPayload -> FlightInfoRegistered(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta,
            departurePoint = FlightPoint.create(
                this.departureCountryCode,
                this.departureAirportCode,
                this.departureZoneId
            ).getOrNull() ?: error("Invalid departurePoint"),
            arrivalPoint = FlightPoint.create(
                this.arrivalCountryCode,
                this.arrivalAirportCode,
                this.arrivalZoneId
            ).getOrNull() ?: error("Invalid arrivalPoint"),
            scheduledDepartureTime = Instant.parse(this.scheduledDepartureTime),
            scheduledArrivalTime = Instant.parse(this.scheduledArrivalTime)
        )
        is FlightDelayedDto -> FlightDelayed(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta,
            estimatedDepartureTime = this.estimatedDepartureTime?.let { Instant.parse(it) },
            estimatedArrivalTime = this.estimatedArrivalTime?.let { Instant.parse(it) }
        )
        is FlightCanceledDto -> FlightCanceled(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta
        )
        is FlightArrivedDto -> FlightArrived(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta
        )
        is FlightStatusUncertainDto -> FlightStatusUncertain(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta,
            reason = this.reason
        )
        is FlightOnScheduleReturnedDto -> FlightOnScheduleReturned(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta
        )
        is FlightMonitoringActivatedDto -> FlightMonitoringActivated(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta
        )
        is FlightMonitoringCompletedDto -> FlightMonitoringCompleted(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta
        )
        is FlightMonitoringFailedDto -> FlightMonitoringFailed(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta,
            reason = this.reason
        )
    }
}
