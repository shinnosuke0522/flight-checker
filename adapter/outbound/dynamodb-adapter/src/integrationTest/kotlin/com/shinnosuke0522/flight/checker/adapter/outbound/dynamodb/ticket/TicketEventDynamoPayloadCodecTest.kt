package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.ticket

import com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.config.DynamoDbCodecTest
import com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.eventstore.EventStoreItem
import com.shinnosuke0522.flight.checker.domain.base.event.CorrelationId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketAnomalyAcknowledged
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketAnomalyRecovered
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
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate

@DynamoDbCodecTest
class TicketEventDynamoPayloadCodecTest : FunSpec() {

    @Autowired
    lateinit var codec: TicketEventDynamoPayloadCodec

    init {
        extension(SpringExtension())

        context("TicketEventDynamoPayloadCodec") {
            test("TicketRegistered のシリアライズとデシリアライズができること") {
                val event = TicketRegistered(
                    id = DomainEventId.generate(),
                    aggregateId = ticketId,
                    sequenceNumber = 1L,
                    meta = DomainEventMeta(
                        occurredAt = Instant.parse("2026-05-01T10:00:00Z"),
                        correlationId = CorrelationId.generate(),
                        causationId = null
                    ),
                    userId = userId,
                    flightIdentity = flightIdentity
                )

                val serialized = codec.serialize()(event)
                val item = EventStoreItem(
                    aggregateId = event.aggregateId.asString(),
                    sequenceNumber = event.sequenceNumber,
                    payload = serialized
                )

                val deserialized = codec.deserialize()(item)
                deserialized shouldBe event
            }

            test("TicketFlightDelayed のシリアライズとデシリアライズができること") {
                val event = TicketFlightDelayed(
                    id = DomainEventId.generate(),
                    aggregateId = ticketId,
                    sequenceNumber = 2L,
                    meta = DomainEventMeta(
                        occurredAt = Instant.parse("2026-05-01T11:00:00Z"),
                        correlationId = CorrelationId.generate(),
                        causationId = null
                    ),
                    detail = AnomalyDelayed("2026-05-01T12:00:00Z")
                )

                val serialized = codec.serialize()(event)
                val item = EventStoreItem(
                    aggregateId = event.aggregateId.asString(),
                    sequenceNumber = event.sequenceNumber,
                    payload = serialized
                )

                val deserialized = codec.deserialize()(item)
                deserialized shouldBe event
            }

            test("TicketFlightCanceled のシリアライズとデシリアライズができること") {
                val event = TicketFlightCanceled(
                    id = DomainEventId.generate(),
                    aggregateId = ticketId,
                    sequenceNumber = 3L,
                    meta = DomainEventMeta(
                        occurredAt = Instant.parse("2026-05-01T12:00:00Z"),
                        correlationId = CorrelationId.generate(),
                        causationId = null
                    )
                )

                val serialized = codec.serialize()(event)
                val item = EventStoreItem(
                    aggregateId = event.aggregateId.asString(),
                    sequenceNumber = event.sequenceNumber,
                    payload = serialized
                )

                val deserialized = codec.deserialize()(item)
                deserialized shouldBe event
            }

            test("TicketFlightUncertain のシリアライズとデシリアライズができること") {
                val event = TicketFlightUncertain(
                    id = DomainEventId.generate(),
                    aggregateId = ticketId,
                    sequenceNumber = 4L,
                    meta = DomainEventMeta(
                        occurredAt = Instant.parse("2026-05-01T13:00:00Z"),
                        correlationId = CorrelationId.generate(),
                        causationId = null
                    ),
                    detail = AnomalyUncertain("Bad weather")
                )

                val serialized = codec.serialize()(event)
                val item = EventStoreItem(
                    aggregateId = event.aggregateId.asString(),
                    sequenceNumber = event.sequenceNumber,
                    payload = serialized
                )

                val deserialized = codec.deserialize()(item)
                deserialized shouldBe event
            }

            test("TicketAnomalyRecovered のシリアライズとデシリアライズができること") {
                val event = TicketAnomalyRecovered(
                    id = DomainEventId.generate(),
                    aggregateId = ticketId,
                    sequenceNumber = 5L,
                    meta = DomainEventMeta(
                        occurredAt = Instant.parse("2026-05-01T14:00:00Z"),
                        correlationId = CorrelationId.generate(),
                        causationId = null
                    )
                )

                val serialized = codec.serialize()(event)
                val item = EventStoreItem(
                    aggregateId = event.aggregateId.asString(),
                    sequenceNumber = event.sequenceNumber,
                    payload = serialized
                )

                val deserialized = codec.deserialize()(item)
                deserialized shouldBe event
            }

            test("TicketAnomalyAcknowledged (AnomalyDelayed) のシリアライズとデシリアライズができること") {
                val event = TicketAnomalyAcknowledged(
                    id = DomainEventId.generate(),
                    aggregateId = ticketId,
                    sequenceNumber = 6L,
                    meta = DomainEventMeta(
                        occurredAt = Instant.parse("2026-05-01T15:00:00Z"),
                        correlationId = CorrelationId.generate(),
                        causationId = null
                    ),
                    acknowledgedAnomaly = AnomalyDelayed("2026-05-01T12:00:00Z")
                )

                val serialized = codec.serialize()(event)
                val item = EventStoreItem(
                    aggregateId = event.aggregateId.asString(),
                    sequenceNumber = event.sequenceNumber,
                    payload = serialized
                )

                val deserialized = codec.deserialize()(item)
                deserialized shouldBe event
            }

            test("TicketAnomalyAcknowledged (AnomalyCanceled) のシリアライズとデシリアライズができること") {
                val event = TicketAnomalyAcknowledged(
                    id = DomainEventId.generate(),
                    aggregateId = ticketId,
                    sequenceNumber = 7L,
                    meta = DomainEventMeta(
                        occurredAt = Instant.parse("2026-05-01T16:00:00Z"),
                        correlationId = CorrelationId.generate(),
                        causationId = null
                    ),
                    acknowledgedAnomaly = AnomalyCanceled
                )

                val serialized = codec.serialize()(event)
                val item = EventStoreItem(
                    aggregateId = event.aggregateId.asString(),
                    sequenceNumber = event.sequenceNumber,
                    payload = serialized
                )

                val deserialized = codec.deserialize()(item)
                deserialized shouldBe event
            }

            test("TicketAnomalyAcknowledged (AnomalyUncertain) のシリアライズとデシリアライズができること") {
                val event = TicketAnomalyAcknowledged(
                    id = DomainEventId.generate(),
                    aggregateId = ticketId,
                    sequenceNumber = 8L,
                    meta = DomainEventMeta(
                        occurredAt = Instant.parse("2026-05-01T17:00:00Z"),
                        correlationId = CorrelationId.generate(),
                        causationId = null
                    ),
                    acknowledgedAnomaly = AnomalyUncertain("Snow")
                )

                val serialized = codec.serialize()(event)
                val item = EventStoreItem(
                    aggregateId = event.aggregateId.asString(),
                    sequenceNumber = event.sequenceNumber,
                    payload = serialized
                )

                val deserialized = codec.deserialize()(item)
                deserialized shouldBe event
            }

            test("TicketFinished のシリアライズとデシリアライズができること") {
                val event = TicketFinished(
                    id = DomainEventId.generate(),
                    aggregateId = ticketId,
                    sequenceNumber = 9L,
                    meta = DomainEventMeta(
                        occurredAt = Instant.parse("2026-05-01T18:00:00Z"),
                        correlationId = CorrelationId.generate(),
                        causationId = null
                    ),
                    reason = FinishReason.ARRIVED
                )

                val serialized = codec.serialize()(event)
                val item = EventStoreItem(
                    aggregateId = event.aggregateId.asString(),
                    sequenceNumber = event.sequenceNumber,
                    payload = serialized
                )

                val deserialized = codec.deserialize()(item)
                deserialized shouldBe event
            }
        }
    }

    companion object {
        val ticketId = TicketId.generate()
        val userId = UserId.generate()
        val flightIdentity = FlightIdentity.create("JL123", LocalDate.of(2026, 5, 1)).getOrNull()!!
    }
}
