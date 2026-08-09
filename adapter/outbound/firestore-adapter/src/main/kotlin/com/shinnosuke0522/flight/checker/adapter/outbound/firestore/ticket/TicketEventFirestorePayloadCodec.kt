package com.shinnosuke0522.flight.checker.adapter.outbound.firestore.ticket

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.shinnosuke0522.flight.checker.adapter.outbound.firestore.eventstore.EventStoreDocument
import com.shinnosuke0522.flight.checker.domain.base.model.CorrelationId
import com.shinnosuke0522.flight.checker.domain.base.model.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.model.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.AnomalyCanceled
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.AnomalyDelayed
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.AnomalyUncertain
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.FinishReason
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.TicketAnomalyAcknowledged
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.TicketAnomalyRecovered
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.TicketEvent
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.TicketFinished
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.TicketFlightCanceled
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.TicketFlightDelayed
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.TicketFlightUncertain
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.TicketId
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.TicketRegistered
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.UserId
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant

@ApplicationScoped
class TicketEventFirestorePayloadCodec(
    private val objectMapper: ObjectMapper
) {
    fun serialize(): (TicketEvent) -> String = { event ->
        objectMapper.writeValueAsString(event.toDto())
    }

    fun deserialize(): (EventStoreDocument) -> TicketEvent = { item ->
        objectMapper.readValue<TicketEventFirestorePayload>(item.payload)
            .toDomain(item.aggregateId, item.sequenceNumber)
    }
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventType"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = TicketRegisteredFirestorePayload::class, name = "TicketRegistered"),
    JsonSubTypes.Type(value = TicketFlightDelayedFirestorePayload::class, name = "TicketFlightDelayed"),
    JsonSubTypes.Type(value = TicketFlightCanceledFirestorePayload::class, name = "TicketFlightCanceled"),
    JsonSubTypes.Type(value = TicketFlightUncertainFirestorePayload::class, name = "TicketFlightUncertain"),
    JsonSubTypes.Type(value = TicketAnomalyRecoveredFirestorePayload::class, name = "TicketAnomalyRecovered"),
    JsonSubTypes.Type(value = TicketAnomalyAcknowledgedFirestorePayload::class, name = "TicketAnomalyAcknowledged"),
    JsonSubTypes.Type(value = TicketFinishedFirestorePayload::class, name = "TicketFinished")
)
sealed interface TicketEventFirestorePayload {
    val id: String
    val occurredAt: String
    val correlationId: String
    val causationId: String?
}

data class TicketRegisteredFirestorePayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?,
    val userId: String,
    val flightIdentity: String
) : TicketEventFirestorePayload

data class TicketFlightDelayedFirestorePayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?,
    val estimatedDepartureTime: String
) : TicketEventFirestorePayload

data class TicketFlightCanceledFirestorePayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?
) : TicketEventFirestorePayload

data class TicketFlightUncertainFirestorePayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?,
    val reason: String
) : TicketEventFirestorePayload

data class TicketAnomalyRecoveredFirestorePayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?
) : TicketEventFirestorePayload

data class TicketAnomalyAcknowledgedFirestorePayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?,
    val anomalyType: String,
    val anomalyValue: String?
) : TicketEventFirestorePayload

data class TicketFinishedFirestorePayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?,
    val reason: String
) : TicketEventFirestorePayload

fun TicketEvent.toDto(): TicketEventFirestorePayload = when (this) {
    is TicketRegistered -> TicketRegisteredFirestorePayload(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value(),
        userId = this.userId.value(),
        flightIdentity = this.flightIdentity.asString()
    )
    is TicketFlightDelayed -> TicketFlightDelayedFirestorePayload(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value(),
        estimatedDepartureTime = this.detail.estimatedDepartureTime
    )
    is TicketFlightCanceled -> TicketFlightCanceledFirestorePayload(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value()
    )
    is TicketFlightUncertain -> TicketFlightUncertainFirestorePayload(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value(),
        reason = this.detail.reason
    )
    is TicketAnomalyRecovered -> TicketAnomalyRecoveredFirestorePayload(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value()
    )
    is TicketAnomalyAcknowledged -> {
        val (type, value) = when (val a = this.acknowledgedAnomaly) {
            is AnomalyCanceled -> "CANCELED" to null
            is AnomalyDelayed -> "DELAYED" to a.estimatedDepartureTime
            is AnomalyUncertain -> "UNCERTAIN" to a.reason
        }
        TicketAnomalyAcknowledgedFirestorePayload(
            id = this.id.value.value(),
            occurredAt = this.meta.occurredAt.toString(),
            correlationId = this.meta.correlationId.value.value(),
            causationId = this.meta.causationId?.value?.value(),
            anomalyType = type,
            anomalyValue = value
        )
    }
    is TicketFinished -> TicketFinishedFirestorePayload(
        id = this.id.value.value(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.value(),
        causationId = this.meta.causationId?.value?.value(),
        reason = this.reason.name
    )
}

@Suppress("CyclomaticComplexMethod")
fun TicketEventFirestorePayload.toDomain(aggregateIdStr: String, sequenceNumber: Long): TicketEvent {
    val domainEventId = DomainEventId.invoke(this.id).getOrNull() ?: error("Invalid id")
    val aggregateId = TicketId.fromString(aggregateIdStr).getOrNull() ?: error("Invalid aggregateId")
    val meta = DomainEventMeta(
        occurredAt = Instant.parse(this.occurredAt),
        correlationId = CorrelationId.invoke(this.correlationId).getOrNull() ?: error("Invalid correlationId"),
        causationId = this.causationId?.let { DomainEventId.invoke(it).getOrNull() ?: error("Invalid causationId") }
    )

    return when (this) {
        is TicketRegisteredFirestorePayload -> TicketRegistered(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta,
            userId = UserId.fromString(this.userId).getOrNull() ?: error("Invalid userId"),
            flightIdentity = FlightIdentity.fromString(this.flightIdentity).getOrNull()
                ?: error("Invalid flightIdentity")
        )
        is TicketFlightDelayedFirestorePayload -> TicketFlightDelayed(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta,
            detail = AnomalyDelayed(estimatedDepartureTime = this.estimatedDepartureTime)
        )
        is TicketFlightCanceledFirestorePayload -> TicketFlightCanceled(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta
        )
        is TicketFlightUncertainFirestorePayload -> TicketFlightUncertain(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta,
            detail = AnomalyUncertain(reason = this.reason)
        )
        is TicketAnomalyRecoveredFirestorePayload -> TicketAnomalyRecovered(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta
        )
        is TicketAnomalyAcknowledgedFirestorePayload -> {
            val anomaly = when (this.anomalyType) {
                "CANCELED" -> AnomalyCanceled
                "DELAYED" -> AnomalyDelayed(this.anomalyValue ?: error("Missing value for DELAYED"))
                "UNCERTAIN" -> AnomalyUncertain(this.anomalyValue ?: error("Missing value for UNCERTAIN"))
                else -> error("Unknown anomaly type: ${this.anomalyType}")
            }
            TicketAnomalyAcknowledged(
                id = domainEventId,
                aggregateId = aggregateId,
                sequenceNumber = sequenceNumber,
                meta = meta,
                acknowledgedAnomaly = anomaly
            )
        }
        is TicketFinishedFirestorePayload -> TicketFinished(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta,
            reason = FinishReason.valueOf(this.reason)
        )
    }
}
