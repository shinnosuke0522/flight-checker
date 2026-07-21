package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.ticket

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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.engine.names.WithDataTestName
import io.kotest.matchers.shouldBe
import java.time.Instant

class TicketEventMappingTest : FunSpec({
    context("TicketEvent のマッピング検証") {
        context("正常系: Event -> DTO -> Event の相互変換ができること") {
            withData(
                TicketEventMappingTestCase(
                    name = "TicketRegistered",
                    event = TicketRegistered(
                        id = domainEventId,
                        aggregateId = aggregateId,
                        sequenceNumber = 1L,
                        meta = meta,
                        userId = userId,
                        flightIdentity = flightIdentity
                    )
                ),
                TicketEventMappingTestCase(
                    name = "TicketFlightDelayed",
                    event = TicketFlightDelayed(
                        id = domainEventId,
                        aggregateId = aggregateId,
                        sequenceNumber = 2L,
                        meta = meta,
                        detail = AnomalyDelayed("2026-05-01T11:00:00Z")
                    )
                ),
                TicketEventMappingTestCase(
                    name = "TicketFlightCanceled",
                    event = TicketFlightCanceled(
                        id = domainEventId,
                        aggregateId = aggregateId,
                        sequenceNumber = 3L,
                        meta = meta
                    )
                ),
                TicketEventMappingTestCase(
                    name = "TicketFlightUncertain",
                    event = TicketFlightUncertain(
                        id = domainEventId,
                        aggregateId = aggregateId,
                        sequenceNumber = 4L,
                        meta = meta,
                        detail = AnomalyUncertain("Weather condition")
                    )
                ),
                TicketEventMappingTestCase(
                    name = "TicketAnomalyRecovered",
                    event = TicketAnomalyRecovered(
                        id = domainEventId,
                        aggregateId = aggregateId,
                        sequenceNumber = 5L,
                        meta = meta
                    )
                ),
                TicketEventMappingTestCase(
                    name = "TicketAnomalyAcknowledged (CANCELED)",
                    event = TicketAnomalyAcknowledged(
                        id = domainEventId,
                        aggregateId = aggregateId,
                        sequenceNumber = 6L,
                        meta = meta,
                        acknowledgedAnomaly = AnomalyCanceled
                    )
                ),
                TicketEventMappingTestCase(
                    name = "TicketAnomalyAcknowledged (DELAYED)",
                    event = TicketAnomalyAcknowledged(
                        id = domainEventId,
                        aggregateId = aggregateId,
                        sequenceNumber = 7L,
                        meta = meta,
                        acknowledgedAnomaly = AnomalyDelayed("2026-05-01T11:00:00Z")
                    )
                ),
                TicketEventMappingTestCase(
                    name = "TicketAnomalyAcknowledged (UNCERTAIN)",
                    event = TicketAnomalyAcknowledged(
                        id = domainEventId,
                        aggregateId = aggregateId,
                        sequenceNumber = 8L,
                        meta = meta,
                        acknowledgedAnomaly = AnomalyUncertain("Unknown Error")
                    )
                ),
                TicketEventMappingTestCase(
                    name = "TicketFinished",
                    event = TicketFinished(
                        id = domainEventId,
                        aggregateId = aggregateId,
                        sequenceNumber = 9L,
                        meta = meta,
                        reason = FinishReason.ARRIVED
                    )
                )
            ) { testCase ->
                val dto = testCase.event.toDto()
                val restored = dto.toDomain(testCase.event.aggregateId.asString(), testCase.event.sequenceNumber)
                restored shouldBe testCase.event
            }
        }

        context("異常系: 不正なDTOをDomainに復元しようとした場合") {
            withData(
                TicketErrorMappingTestCase(
                    name = "不正な DomainEventId",
                    payload = TicketFlightCanceledDynamoPayload(
                        id = "", // Invalid
                        occurredAt = Instant.now().toString(),
                        correlationId = meta.correlationId.value.toString(),
                        causationId = null
                    ),
                    aggregateIdStr = aggregateId.asString(),
                    sequenceNumber = 1L
                ),
                TicketErrorMappingTestCase(
                    name = "不正な AggregateId (TicketId)",
                    payload = TicketFlightCanceledDynamoPayload(
                        id = domainEventId.value.toString(),
                        occurredAt = Instant.now().toString(),
                        correlationId = meta.correlationId.value.toString(),
                        causationId = null
                    ),
                    aggregateIdStr = "", // Invalid
                    sequenceNumber = 1L
                ),
                TicketErrorMappingTestCase(
                    name = "不正な UserId",
                    payload = TicketRegisteredDynamoPayload(
                        id = domainEventId.value.toString(),
                        occurredAt = Instant.now().toString(),
                        correlationId = meta.correlationId.value.toString(),
                        causationId = null,
                        userId = "", // Invalid
                        flightIdentity = flightIdentity.asString()
                    ),
                    aggregateIdStr = aggregateId.asString(),
                    sequenceNumber = 1L
                ),
                TicketErrorMappingTestCase(
                    name = "AnomalyAcknowledged で未知の type",
                    payload = TicketAnomalyAcknowledgedDynamoPayload(
                        id = domainEventId.value.toString(),
                        occurredAt = Instant.now().toString(),
                        correlationId = meta.correlationId.value.toString(),
                        causationId = null,
                        anomalyType = "UNKNOWN_TYPE",
                        anomalyValue = null
                    ),
                    aggregateIdStr = aggregateId.asString(),
                    sequenceNumber = 1L
                )
            ) { testCase ->
                shouldThrow<Exception> {
                    testCase.payload.toDomain(testCase.aggregateIdStr, testCase.sequenceNumber)
                }
            }
        }
    }
}) {
    companion object {
        val domainEventId = DomainEventId.invoke("01H9YXP882K1G2VQ6Q5YJ50J9P").fold({ error(it.toString()) }, { it })
        val aggregateId = TicketId.fromString("01H9YXP882K1G2VQ6Q5YJ50J9R").fold({ error(it.toString()) }, { it })
        val meta = DomainEventMeta(
            occurredAt = Instant.parse("2026-05-01T00:00:00Z"),
            correlationId = CorrelationId.invoke("01H9YXP882K1G2VQ6Q5YJ50J9Q").fold({ error(it.toString()) }, { it }),
            causationId = null
        )
        val userId = UserId.fromString("01H9YXP882K1G2VQ6Q5YJ50J9S").fold({ error(it.toString()) }, { it })
        val flightIdentity = FlightIdentity.create(
            "JL123",
            java.time.LocalDate.of(2026, 5, 1)
        ).fold({ error(it.toString()) }, { it })
    }
}

data class TicketEventMappingTestCase(
    val name: String,
    val event: TicketEvent
) : WithDataTestName {
    override fun dataTestName() = name
}

data class TicketErrorMappingTestCase(
    val name: String,
    val payload: TicketEventDynamoPayload,
    val aggregateIdStr: String,
    val sequenceNumber: Long
) : WithDataTestName {
    override fun dataTestName() = name
}
