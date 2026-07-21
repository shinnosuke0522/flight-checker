package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.ticket

import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.ticket.model.AcknowledgedTicket
import com.shinnosuke0522.flight.checker.domain.ticket.model.AlertTicket
import com.shinnosuke0522.flight.checker.domain.ticket.model.AnomalyCanceled
import com.shinnosuke0522.flight.checker.domain.ticket.model.AnomalyDelayed
import com.shinnosuke0522.flight.checker.domain.ticket.model.AnomalyUncertain
import com.shinnosuke0522.flight.checker.domain.ticket.model.FinishReason
import com.shinnosuke0522.flight.checker.domain.ticket.model.FinishedTicket
import com.shinnosuke0522.flight.checker.domain.ticket.model.NormalTicket
import com.shinnosuke0522.flight.checker.domain.ticket.model.Ticket
import com.shinnosuke0522.flight.checker.domain.ticket.model.TicketId
import com.shinnosuke0522.flight.checker.domain.ticket.model.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.engine.names.WithDataTestName
import io.kotest.matchers.shouldBe

class TicketSnapshotMappingTest : FunSpec({
    context("TicketSnapshotDynamoItem のマッピング検証") {
        context("正常系: Domain -> Item -> Domain の相互変換ができること") {
            withData(
                TicketSnapshotMappingTestCase(
                    name = "NormalTicket",
                    domain = NormalTicket(
                        id = ticketId,
                        version = version,
                        userId = userId,
                        flightIdentity = flightIdentity
                    )
                ),
                TicketSnapshotMappingTestCase(
                    name = "AlertTicket (CANCELED)",
                    domain = AlertTicket(
                        id = ticketId,
                        version = version,
                        userId = userId,
                        flightIdentity = flightIdentity,
                        currentAnomaly = AnomalyCanceled
                    )
                ),
                TicketSnapshotMappingTestCase(
                    name = "AlertTicket (DELAYED)",
                    domain = AlertTicket(
                        id = ticketId,
                        version = version,
                        userId = userId,
                        flightIdentity = flightIdentity,
                        currentAnomaly = AnomalyDelayed("2026-05-01T11:00:00Z")
                    )
                ),
                TicketSnapshotMappingTestCase(
                    name = "AlertTicket (UNCERTAIN)",
                    domain = AlertTicket(
                        id = ticketId,
                        version = version,
                        userId = userId,
                        flightIdentity = flightIdentity,
                        currentAnomaly = AnomalyUncertain("Weather condition")
                    )
                ),
                TicketSnapshotMappingTestCase(
                    name = "AcknowledgedTicket",
                    domain = AcknowledgedTicket(
                        id = ticketId,
                        version = version,
                        userId = userId,
                        flightIdentity = flightIdentity,
                        acknowledgedAnomaly = AnomalyCanceled
                    )
                ),
                TicketSnapshotMappingTestCase(
                    name = "FinishedTicket",
                    domain = FinishedTicket(
                        id = ticketId,
                        version = version,
                        userId = userId,
                        flightIdentity = flightIdentity,
                        reason = FinishReason.ARRIVED
                    )
                )
            ) { testCase ->
                val item = TicketSnapshotDynamoItem.fromDomain(testCase.domain)
                val restored = item.toDomain()
                restored shouldBe testCase.domain
            }
        }

        context("異常系: 不正な Item を Domain に復元しようとした場合") {
            withData(
                TicketSnapshotErrorMappingTestCase(
                    name = "未知の TicketType",
                    item = TicketSnapshotDynamoItem(
                        id = ticketId.asString(),
                        version = version.value,
                        userId = userId.value(),
                        flightIdentity = flightIdentity.asString(),
                        type = "UnknownType",
                        currentAnomaly = null,
                        acknowledgedAnomaly = null,
                        reason = null
                    )
                ),
                TicketSnapshotErrorMappingTestCase(
                    name = "AlertTicket で currentAnomaly が null",
                    item = TicketSnapshotDynamoItem(
                        id = ticketId.asString(),
                        version = version.value,
                        userId = userId.value(),
                        flightIdentity = flightIdentity.asString(),
                        type = "Alert",
                        currentAnomaly = null, // Invalid
                        acknowledgedAnomaly = null,
                        reason = null
                    )
                ),
                TicketSnapshotErrorMappingTestCase(
                    name = "AlertTicket で 未知の Anomaly 形式",
                    item = TicketSnapshotDynamoItem(
                        id = ticketId.asString(),
                        version = version.value,
                        userId = userId.value(),
                        flightIdentity = flightIdentity.asString(),
                        type = "Alert",
                        currentAnomaly = "UNKNOWN_ANOMALY:data", // Invalid
                        acknowledgedAnomaly = null,
                        reason = null
                    )
                ),
                TicketSnapshotErrorMappingTestCase(
                    name = "FinishedTicket で reason が null",
                    item = TicketSnapshotDynamoItem(
                        id = ticketId.asString(),
                        version = version.value,
                        userId = userId.value(),
                        flightIdentity = flightIdentity.asString(),
                        type = "Finished",
                        currentAnomaly = null,
                        acknowledgedAnomaly = null,
                        reason = null // Invalid
                    )
                )
            ) { testCase ->
                shouldThrow<Exception> {
                    testCase.item.toDomain()
                }
            }
        }
    }
}) {
    companion object {
        val ticketId = TicketId.fromString("01H9YXP882K1G2VQ6Q5YJ50J9R").fold({ error(it.toString()) }, { it })
        val version = AggregateVersion(1L)
        val userId = UserId.fromString("01H9YXP882K1G2VQ6Q5YJ50J9S").fold({ error(it.toString()) }, { it })
        val flightIdentity = FlightIdentity.create("JL123", java.time.LocalDate.of(2026, 5, 1)).fold({
            error(it.toString())
        }, { it })
    }
}

data class TicketSnapshotMappingTestCase(
    val name: String,
    val domain: Ticket
) : WithDataTestName {
    override fun dataTestName() = name
}

data class TicketSnapshotErrorMappingTestCase(
    val name: String,
    val item: TicketSnapshotDynamoItem
) : WithDataTestName {
    override fun dataTestName() = name
}
