package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.flight

import com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.config.DynamoDbCodecTest
import com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.eventstore.EventStoreItem
import com.shinnosuke0522.flight.checker.domain.base.event.CorrelationId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightArrived
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightCanceled
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightDelayed
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightInfoRegistered
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightMonitoringActivated
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightMonitoringCompleted
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightMonitoringFailed
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightOnScheduleReturned
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightStatusUncertain
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightPoint
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate

@DynamoDbCodecTest
class FlightInfoEventDynamoPayloadCodecTest : FunSpec() {

    @Autowired
    lateinit var codec: FlightInfoEventDynamoPayloadCodec

    init {
        extension(SpringExtension())

        context("FlightInfoEventDynamoPayloadCodec") {
            test("FlightInfoRegistered のシリアライズとデシリアライズができること") {
                val event = FlightInfoRegistered(
                    id = DomainEventId.generate(),
                    aggregateId = flightIdentity,
                    sequenceNumber = 1L,
                    meta = DomainEventMeta(
                        occurredAt = Instant.parse("2026-05-01T10:00:00Z"),
                        correlationId = CorrelationId.generate(),
                        causationId = null
                    ),
                    departurePoint = departurePoint,
                    arrivalPoint = arrivalPoint,
                    scheduledDepartureTime = Instant.parse("2026-05-01T12:00:00Z"),
                    scheduledArrivalTime = Instant.parse("2026-05-01T15:00:00Z")
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

            test("FlightDelayed のシリアライズとデシリアライズができること") {
                val event = FlightDelayed(
                    id = DomainEventId.generate(),
                    aggregateId = flightIdentity,
                    sequenceNumber = 2L,
                    meta = DomainEventMeta(
                        occurredAt = Instant.parse("2026-05-01T11:00:00Z"),
                        correlationId = CorrelationId.generate(),
                        causationId = null
                    ),
                    estimatedDepartureTime = Instant.parse("2026-05-01T13:00:00Z"),
                    estimatedArrivalTime = Instant.parse("2026-05-01T16:00:00Z")
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

            test("FlightCanceled のシリアライズとデシリアライズができること") {
                val event = FlightCanceled(
                    id = DomainEventId.generate(),
                    aggregateId = flightIdentity,
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

            test("FlightArrived のシリアライズとデシリアライズができること") {
                val event = FlightArrived(
                    id = DomainEventId.generate(),
                    aggregateId = flightIdentity,
                    sequenceNumber = 4L,
                    meta = DomainEventMeta(
                        occurredAt = Instant.parse("2026-05-01T15:00:00Z"),
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

            test("FlightStatusUncertain のシリアライズとデシリアライズができること") {
                val event = FlightStatusUncertain(
                    id = DomainEventId.generate(),
                    aggregateId = flightIdentity,
                    sequenceNumber = 5L,
                    meta = DomainEventMeta(
                        occurredAt = Instant.parse("2026-05-01T16:00:00Z"),
                        correlationId = CorrelationId.generate(),
                        causationId = null
                    ),
                    reason = "Bad weather"
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

            test("FlightOnScheduleReturned のシリアライズとデシリアライズができること") {
                val event = FlightOnScheduleReturned(
                    id = DomainEventId.generate(),
                    aggregateId = flightIdentity,
                    sequenceNumber = 6L,
                    meta = DomainEventMeta(
                        occurredAt = Instant.parse("2026-05-01T17:00:00Z"),
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

            test("FlightMonitoringActivated のシリアライズとデシリアライズができること") {
                val event = FlightMonitoringActivated(
                    id = DomainEventId.generate(),
                    aggregateId = flightIdentity,
                    sequenceNumber = 7L,
                    meta = DomainEventMeta(
                        occurredAt = Instant.parse("2026-05-01T18:00:00Z"),
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

            test("FlightMonitoringCompleted のシリアライズとデシリアライズができること") {
                val event = FlightMonitoringCompleted(
                    id = DomainEventId.generate(),
                    aggregateId = flightIdentity,
                    sequenceNumber = 8L,
                    meta = DomainEventMeta(
                        occurredAt = Instant.parse("2026-05-01T19:00:00Z"),
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

            test("FlightMonitoringFailed のシリアライズとデシリアライズができること") {
                val event = FlightMonitoringFailed(
                    id = DomainEventId.generate(),
                    aggregateId = flightIdentity,
                    sequenceNumber = 9L,
                    meta = DomainEventMeta(
                        occurredAt = Instant.parse("2026-05-01T20:00:00Z"),
                        correlationId = CorrelationId.generate(),
                        causationId = null
                    ),
                    reason = "API error"
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
        val flightIdentity = FlightIdentity.create("JL123", LocalDate.of(2026, 5, 1)).getOrNull()!!
        val departurePoint = FlightPoint.create("JP", "HND", "Asia/Tokyo").getOrNull()!!
        val arrivalPoint = FlightPoint.create("US", "JFK", "America/New_York").getOrNull()!!
    }
}
