package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.flight

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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.engine.names.WithDataTestName
import io.kotest.matchers.shouldBe
import java.time.Instant

class FlightInfoEventMappingTest : FunSpec({
    context("FlightInfoEvent のマッピング検証") {
        context("正常系: Event -> DTO -> Event の相互変換ができること") {
            withData(
                EventMappingTestCase(
                    name = "FlightInfoRegistered",
                    event = FlightInfoRegistered(
                        id = domainEventId,
                        aggregateId = aggregateId,
                        sequenceNumber = 1L,
                        meta = meta,
                        departurePoint = departurePoint,
                        arrivalPoint = arrivalPoint,
                        scheduledDepartureTime = Instant.parse("2026-05-01T10:00:00Z"),
                        scheduledArrivalTime = Instant.parse("2026-05-01T12:00:00Z")
                    )
                ),
                EventMappingTestCase(
                    name = "FlightDelayed",
                    event = FlightDelayed(
                        id = domainEventId,
                        aggregateId = aggregateId,
                        sequenceNumber = 2L,
                        meta = meta,
                        estimatedDepartureTime = Instant.parse("2026-05-01T11:00:00Z"),
                        estimatedArrivalTime = Instant.parse("2026-05-01T13:00:00Z")
                    )
                ),
                EventMappingTestCase(
                    name = "FlightCanceled",
                    event = FlightCanceled(
                        id = domainEventId,
                        aggregateId = aggregateId,
                        sequenceNumber = 3L,
                        meta = meta
                    )
                ),
                EventMappingTestCase(
                    name = "FlightArrived",
                    event = FlightArrived(
                        id = domainEventId,
                        aggregateId = aggregateId,
                        sequenceNumber = 4L,
                        meta = meta
                    )
                ),
                EventMappingTestCase(
                    name = "FlightStatusUncertain",
                    event = FlightStatusUncertain(
                        id = domainEventId,
                        aggregateId = aggregateId,
                        sequenceNumber = 5L,
                        meta = meta,
                        reason = "Weather condition"
                    )
                ),
                EventMappingTestCase(
                    name = "FlightOnScheduleReturned",
                    event = FlightOnScheduleReturned(
                        id = domainEventId,
                        aggregateId = aggregateId,
                        sequenceNumber = 6L,
                        meta = meta
                    )
                ),
                EventMappingTestCase(
                    name = "FlightMonitoringActivated",
                    event = FlightMonitoringActivated(
                        id = domainEventId,
                        aggregateId = aggregateId,
                        sequenceNumber = 7L,
                        meta = meta
                    )
                ),
                EventMappingTestCase(
                    name = "FlightMonitoringCompleted",
                    event = FlightMonitoringCompleted(
                        id = domainEventId,
                        aggregateId = aggregateId,
                        sequenceNumber = 8L,
                        meta = meta
                    )
                ),
                EventMappingTestCase(
                    name = "FlightMonitoringFailed",
                    event = FlightMonitoringFailed(
                        id = domainEventId,
                        aggregateId = aggregateId,
                        sequenceNumber = 9L,
                        meta = meta,
                        reason = "API error"
                    )
                )
            ) { testCase ->
                val dto = testCase.event.toDto()
                val restored = runCatching {
                    dto.toDomain(testCase.event.aggregateId.asString(), testCase.event.sequenceNumber)
                }.onFailure { println("MAPPING ERROR: ${it.message}") }.getOrThrow()
                restored shouldBe testCase.event
            }
        }

        context("異常系: 不正なDTOをDomainに復元しようとした場合") {
            withData(
                ErrorMappingTestCase(
                    name = "不正な DomainEventId",
                    payload = FlightCanceledDto(
                        id = "", // Invalid
                        occurredAt = Instant.now().toString(),
                        correlationId = meta.correlationId.value.toString(),
                        causationId = null
                    ),
                    aggregateIdStr = aggregateId.asString(),
                    sequenceNumber = 1L
                ),
                ErrorMappingTestCase(
                    name = "不正な AggregateId",
                    payload = FlightCanceledDto(
                        id = domainEventId.value.toString(),
                        occurredAt = Instant.now().toString(),
                        correlationId = meta.correlationId.value.toString(),
                        causationId = null
                    ),
                    aggregateIdStr = "", // Invalid
                    sequenceNumber = 1L
                ),
                ErrorMappingTestCase(
                    name = "不正な CorrelationId",
                    payload = FlightCanceledDto(
                        id = domainEventId.value.toString(),
                        occurredAt = Instant.now().toString(),
                        correlationId = "", // Invalid
                        causationId = null
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
        val aggregateId = FlightIdentity.create(
            "JL123",
            java.time.LocalDate.of(2026, 5, 1)
        ).fold({ error(it.toString()) }, { it })
        val meta = DomainEventMeta(
            occurredAt = Instant.parse("2026-05-01T00:00:00Z"),
            correlationId = CorrelationId.invoke("01H9YXP882K1G2VQ6Q5YJ50J9Q").fold({ error(it.toString()) }, { it }),
            causationId = null
        )
        val departurePoint = FlightPoint.create("JP", "HND", "Asia/Tokyo").fold({ error(it.toString()) }, { it })
        val arrivalPoint = FlightPoint.create("US", "JFK", "America/New_York").fold({ error(it.toString()) }, { it })
    }
}

data class EventMappingTestCase(
    val name: String,
    val event: FlightInfoEvent
) : WithDataTestName {
    override fun dataTestName() = name
}

data class ErrorMappingTestCase(
    val name: String,
    val payload: FlightInfoEventDynamoPayload,
    val aggregateIdStr: String,
    val sequenceNumber: Long
) : WithDataTestName {
    override fun dataTestName() = name
}
