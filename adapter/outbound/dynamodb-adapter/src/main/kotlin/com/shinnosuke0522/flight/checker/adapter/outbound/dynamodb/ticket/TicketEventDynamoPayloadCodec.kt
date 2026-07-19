package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.ticket

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.eventstore.EventStoreItem
import com.shinnosuke0522.flight.checker.domain.base.event.CorrelationId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketAnomalyAcknowledged
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketAnomalyRecovered
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketEvent
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFinished
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFlightCanceled
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFlightDelayed
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFlightUncertain
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketRegistered
import com.shinnosuke0522.flight.checker.domain.ticket.model.AnomalyCanceled
import com.shinnosuke0522.flight.checker.domain.ticket.model.AnomalyDelayed
import com.shinnosuke0522.flight.checker.domain.ticket.model.AnomalyUncertain
import com.shinnosuke0522.flight.checker.domain.ticket.model.FinishReason
import com.shinnosuke0522.flight.checker.domain.ticket.model.TicketId
import com.shinnosuke0522.flight.checker.domain.ticket.model.UserId
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class TicketEventDynamoPayloadCodec(
    private val objectMapper: ObjectMapper
) {
    fun serialize(): (TicketEvent) -> String = { event ->
        objectMapper.writeValueAsString(event.toDto())
    }

    fun deserialize(): (EventStoreItem) -> TicketEvent = { item ->
        objectMapper.readValue<TicketEventDynamoPayload>(item.payload)
            .toDomain(item.aggregateId, item.sequenceNumber)
    }
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventType"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = TicketRegisteredDynamoPayload::class, name = "TicketRegistered"),
    JsonSubTypes.Type(value = TicketFlightDelayedDynamoPayload::class, name = "TicketFlightDelayed"),
    JsonSubTypes.Type(value = TicketFlightCanceledDynamoPayload::class, name = "TicketFlightCanceled"),
    JsonSubTypes.Type(value = TicketFlightUncertainDynamoPayload::class, name = "TicketFlightUncertain"),
    JsonSubTypes.Type(value = TicketAnomalyRecoveredDynamoPayload::class, name = "TicketAnomalyRecovered"),
    JsonSubTypes.Type(value = TicketAnomalyAcknowledgedDynamoPayload::class, name = "TicketAnomalyAcknowledged"),
    JsonSubTypes.Type(value = TicketFinishedDynamoPayload::class, name = "TicketFinished")
)
sealed interface TicketEventDynamoPayload {
    val id: String
    val occurredAt: String
    val correlationId: String
    val causationId: String?
}

data class TicketRegisteredDynamoPayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?,
    val userId: String,
    val flightIdentity: String
) : TicketEventDynamoPayload

data class TicketFlightDelayedDynamoPayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?,
    val estimatedDepartureTime: String
) : TicketEventDynamoPayload

data class TicketFlightCanceledDynamoPayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?
) : TicketEventDynamoPayload

data class TicketFlightUncertainDynamoPayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?,
    val reason: String
) : TicketEventDynamoPayload

data class TicketAnomalyRecoveredDynamoPayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?
) : TicketEventDynamoPayload

data class TicketAnomalyAcknowledgedDynamoPayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?,
    val anomalyType: String,
    val anomalyValue: String?
) : TicketEventDynamoPayload

data class TicketFinishedDynamoPayload(
    override val id: String,
    override val occurredAt: String,
    override val correlationId: String,
    override val causationId: String?,
    val reason: String
) : TicketEventDynamoPayload

fun TicketEvent.toDto(): TicketEventDynamoPayload = when (this) {
    is TicketRegistered -> TicketRegisteredDynamoPayload(
        id = this.id.value.toString(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.toString(),
        causationId = this.meta.causationId?.value?.toString(),
        userId = this.userId.value(),
        flightIdentity = this.flightIdentity.asString()
    )
    is TicketFlightDelayed -> TicketFlightDelayedDynamoPayload(
        id = this.id.value.toString(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.toString(),
        causationId = this.meta.causationId?.value?.toString(),
        estimatedDepartureTime = this.detail.estimatedDepartureTime
    )
    is TicketFlightCanceled -> TicketFlightCanceledDynamoPayload(
        id = this.id.value.toString(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.toString(),
        causationId = this.meta.causationId?.value?.toString()
    )
    is TicketFlightUncertain -> TicketFlightUncertainDynamoPayload(
        id = this.id.value.toString(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.toString(),
        causationId = this.meta.causationId?.value?.toString(),
        reason = this.detail.reason
    )
    is TicketAnomalyRecovered -> TicketAnomalyRecoveredDynamoPayload(
        id = this.id.value.toString(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.toString(),
        causationId = this.meta.causationId?.value?.toString()
    )
    is TicketAnomalyAcknowledged -> {
        val (type, value) = when (val a = this.acknowledgedAnomaly) {
            is AnomalyCanceled -> "CANCELED" to null
            is AnomalyDelayed -> "DELAYED" to a.estimatedDepartureTime
            is AnomalyUncertain -> "UNCERTAIN" to a.reason
        }
        TicketAnomalyAcknowledgedDynamoPayload(
            id = this.id.value.toString(),
            occurredAt = this.meta.occurredAt.toString(),
            correlationId = this.meta.correlationId.value.toString(),
            causationId = this.meta.causationId?.value?.toString(),
            anomalyType = type,
            anomalyValue = value
        )
    }
    is TicketFinished -> TicketFinishedDynamoPayload(
        id = this.id.value.toString(),
        occurredAt = this.meta.occurredAt.toString(),
        correlationId = this.meta.correlationId.value.toString(),
        causationId = this.meta.causationId?.value?.toString(),
        reason = this.reason.name
    )
}

@Suppress("CyclomaticComplexMethod")
fun TicketEventDynamoPayload.toDomain(aggregateIdStr: String, sequenceNumber: Long): TicketEvent {
    val domainEventId = DomainEventId.invoke(this.id).getOrNull() ?: error("Invalid id")
    val aggregateId = TicketId.fromString(aggregateIdStr).getOrNull() ?: error("Invalid aggregateId")
    val meta = DomainEventMeta(
        occurredAt = Instant.parse(this.occurredAt),
        correlationId = CorrelationId.invoke(this.correlationId).getOrNull() ?: error("Invalid correlationId"),
        causationId = this.causationId?.let { DomainEventId.invoke(it).getOrNull() ?: error("Invalid causationId") }
    )

    return when (this) {
        is TicketRegisteredDynamoPayload -> TicketRegistered(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta,
            userId = UserId.fromString(this.userId).getOrNull() ?: error("Invalid userId"),
            flightIdentity = FlightIdentity.fromString(this.flightIdentity).getOrNull()
                ?: error("Invalid flightIdentity")
        )
        is TicketFlightDelayedDynamoPayload -> TicketFlightDelayed(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta,
            detail = AnomalyDelayed(estimatedDepartureTime = this.estimatedDepartureTime)
        )
        is TicketFlightCanceledDynamoPayload -> TicketFlightCanceled(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta
        )
        is TicketFlightUncertainDynamoPayload -> TicketFlightUncertain(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta,
            detail = AnomalyUncertain(reason = this.reason)
        )
        is TicketAnomalyRecoveredDynamoPayload -> TicketAnomalyRecovered(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta
        )
        is TicketAnomalyAcknowledgedDynamoPayload -> {
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
        is TicketFinishedDynamoPayload -> TicketFinished(
            id = domainEventId,
            aggregateId = aggregateId,
            sequenceNumber = sequenceNumber,
            meta = meta,
            reason = FinishReason.valueOf(this.reason)
        )
    }
}
