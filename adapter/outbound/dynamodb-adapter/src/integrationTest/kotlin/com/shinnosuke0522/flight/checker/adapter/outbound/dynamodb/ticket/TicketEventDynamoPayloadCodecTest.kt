package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.ticket

import com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.config.DynamoDbCodecTest
import com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.eventstore.EventStoreItem
import com.shinnosuke0522.flight.checker.domain.base.event.CorrelationId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketAnomalyAcknowledged
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketRegistered
import com.shinnosuke0522.flight.checker.domain.ticket.model.AnomalyDelayed
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

            test("TicketAnomalyAcknowledged のシリアライズとデシリアライズができること") {
                val event = TicketAnomalyAcknowledged(
                    id = DomainEventId.generate(),
                    aggregateId = ticketId,
                    sequenceNumber = 2L,
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
        }
    }

    companion object {
        val ticketId = TicketId.generate()
        val userId = UserId.generate()
        val flightIdentity = FlightIdentity.create("JL123", LocalDate.of(2026, 5, 1)).getOrNull()!!
    }
}
