package com.shinnosuke0522.flight.checker.adapter.outbound.firestore.flight

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.shinnosuke0522.flight.checker.adapter.outbound.firestore.eventstore.EventStoreDocument
import com.shinnosuke0522.flight.checker.domain.base.model.CorrelationId
import com.shinnosuke0522.flight.checker.domain.base.model.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.model.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightArrived
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightCanceled
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightDelayed
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfoEvent
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfoRegistered
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightMonitoringActivated
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightMonitoringCompleted
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightMonitoringFailed
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightOnScheduleReturned
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightStatusUncertain
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightPoint
import java.time.Instant

class FlightInfoEventFirestorePayloadCodec(
    private val objectMapper: ObjectMapper
) {
    fun serialize(): (FlightInfoEvent) -> String = { event ->
        objectMapper.writeValueAsString(event.toDto())
    }

    fun deserialize(): (EventStoreDocument) -> FlightInfoEvent = { item ->
        objectMapper.readValue<FlightInfoEventFirestorePayload>(item.payload)
            .toDomain(item.aggregateId, item.sequenceNumber)
    }
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventType"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = FlightInfoRegisteredFirestorePayload::class, name = "FlightInfoRegistered"),
    JsonSubTypes.Type(value = FlightDelayedFirestorePayload::class, name = "FlightDelayed"),
    JsonSubTypes.Type(value = FlightCanceledFirestorePayload::class, name = "FlightCanceled"),
    JsonSubTypes.Type(value = FlightArrivedFirestorePayload::class, name = "FlightArrived"),
    JsonSubTypes.Type(value = FlightStatusUncertainFirestorePayload::class, name = "FlightStatusUncertain"),
    JsonSubTypes.Type(value = FlightOnScheduleReturnedFirestorePayload::class, name = "FlightOnScheduleReturned"),
    JsonSubTypes.Type(value = FlightMonitoringActivatedFirestorePayload::class, name = "FlightMonitoringActivated"),
    JsonSubTypes.Type(value = FlightMonitoringCompletedFirestorePayload::class, name = "FlightMonitoringCompleted"),
    JsonSubTypes.Type(value = FlightMonitoringFailedFirestorePayload::class, name = "FlightMonitoringFailed")
)
sealed interface FlightInfoEventFirestorePayload {
    val id: String
    val occurredAt: String
    val correlationId: String
    val causationId: String?
}

data class FlightInfoRegisteredFirestorePayload(
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
) : FlightInfoEventFirestorePayload

data class FlightDelayedFirestorePayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?,
    val estimatedDepartureTime: String?,
    val estimatedArrivalTime: String?
) : FlightInfoEventFirestorePayload

data class FlightCanceledFirestorePayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?
) : FlightInfoEventFirestorePayload

data class FlightArrivedFirestorePayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?
) : FlightInfoEventFirestorePayload

data class FlightStatusUncertainFirestorePayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?,
    val reason: String
) : FlightInfoEventFirestorePayload

data class FlightOnScheduleReturnedFirestorePayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?
) : FlightInfoEventFirestorePayload

data class FlightMonitoringActivatedFirestorePayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?
) : FlightInfoEventFirestorePayload

data class FlightMonitoringCompletedFirestorePayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?
) : FlightInfoEventFirestorePayload

data class FlightMonitoringFailedFirestorePayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?,
    val reason: String
) : FlightInfoEventFirestorePayload

fun FlightInfoEvent.toDto(): FlightInfoEventFirestorePayload = when (this) {
    is FlightInfoRegistered -> FlightInfoRegisteredFirestorePayload(
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
    is FlightDelayed -> FlightDelayedFirestorePayload(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value(),
        estimatedDepartureTime = this.estimatedDepartureTime?.toString(),
        estimatedArrivalTime = this.estimatedArrivalTime?.toString()
    )
    is FlightCanceled -> FlightCanceledFirestorePayload(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value()
    )
    is FlightArrived -> FlightArrivedFirestorePayload(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value()
    )
    is FlightStatusUncertain -> FlightStatusUncertainFirestorePayload(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value(),
        reason = this.reason
    )
    is FlightOnScheduleReturned -> FlightOnScheduleReturnedFirestorePayload(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value()
    )
    is FlightMonitoringActivated -> FlightMonitoringActivatedFirestorePayload(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value()
    )
    is FlightMonitoringCompleted -> FlightMonitoringCompletedFirestorePayload(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value()
    )
    is FlightMonitoringFailed -> FlightMonitoringFailedFirestorePayload(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value(),
        reason = this.reason
    )
}

@Suppress("CyclomaticComplexMethod")
fun FlightInfoEventFirestorePayload.toDomain(aggregateIdStr: String, sequenceNumber: Long): FlightInfoEvent {
    val domainEventId = DomainEventId.invoke(this.id).getOrNull() ?: error("Invalid id")
    val aggregateId = FlightIdentity.fromString(aggregateIdStr).getOrNull() ?: error("Invalid aggregateId")
    val meta = DomainEventMeta(
        occurredAt = Instant.parse(this.occurredAt),
        correlationId = CorrelationId.invoke(this.correlationId).getOrNull() ?: error("Invalid correlationId"),
        causationId = this.causationId?.let { DomainEventId.invoke(it).getOrNull() ?: error("Invalid causationId") }
    )

    return when (this) {
        is FlightInfoRegisteredFirestorePayload -> FlightInfoRegistered(
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
        is FlightDelayedFirestorePayload -> FlightDelayed(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta,
            estimatedDepartureTime = this.estimatedDepartureTime?.let { Instant.parse(it) },
            estimatedArrivalTime = this.estimatedArrivalTime?.let { Instant.parse(it) }
        )
        is FlightCanceledFirestorePayload -> FlightCanceled(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta
        )
        is FlightArrivedFirestorePayload -> FlightArrived(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta
        )
        is FlightStatusUncertainFirestorePayload -> FlightStatusUncertain(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta,
            reason = this.reason
        )
        is FlightOnScheduleReturnedFirestorePayload -> FlightOnScheduleReturned(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta
        )
        is FlightMonitoringActivatedFirestorePayload -> FlightMonitoringActivated(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta
        )
        is FlightMonitoringCompletedFirestorePayload -> FlightMonitoringCompleted(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta
        )
        is FlightMonitoringFailedFirestorePayload -> FlightMonitoringFailed(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta,
            reason = this.reason
        )
    }
}
