package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.ticket

import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.ticket.model.AcknowledgedTicket
import com.shinnosuke0522.flight.checker.domain.ticket.model.AlertTicket
import com.shinnosuke0522.flight.checker.domain.ticket.model.Anomaly
import com.shinnosuke0522.flight.checker.domain.ticket.model.AnomalyCanceled
import com.shinnosuke0522.flight.checker.domain.ticket.model.AnomalyDelayed
import com.shinnosuke0522.flight.checker.domain.ticket.model.AnomalyUncertain
import com.shinnosuke0522.flight.checker.domain.ticket.model.FinishReason
import com.shinnosuke0522.flight.checker.domain.ticket.model.FinishedTicket
import com.shinnosuke0522.flight.checker.domain.ticket.model.NormalTicket
import com.shinnosuke0522.flight.checker.domain.ticket.model.Ticket
import com.shinnosuke0522.flight.checker.domain.ticket.model.TicketId
import com.shinnosuke0522.flight.checker.domain.ticket.model.UserId
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey

@Suppress("DataClassShouldBeImmutable")
@DynamoDbBean
data class TicketSnapshotDynamoItem(
    @get:DynamoDbPartitionKey
    var id: String = "",
    @get:DynamoDbVersionAttribute
    var version: Long? = null,
    var userId: String = "",
    var flightIdentity: String = "",
    var type: String = "",
    var currentAnomaly: String? = null,
    var acknowledgedAnomaly: String? = null,
    var reason: String? = null
) {
    fun toDomain(): Ticket {
        val ticketId = TicketId.fromString(id)
            .fold({ error(it.toString()) }, { it })
        val aggregateVersion = AggregateVersion(version ?: 1L)
        val uid = UserId.fromString(userId)
            .fold({ error(it.toString()) }, { it })

        val identity = FlightIdentity.fromString(flightIdentity)
            .fold({ error(it.toString()) }, { it })

        return when (type) {
            "Normal" -> NormalTicket(
                id = ticketId,
                version = aggregateVersion,
                userId = uid,
                flightIdentity = identity
            )
            "Alert" -> AlertTicket(
                id = ticketId,
                version = aggregateVersion,
                userId = uid,
                flightIdentity = identity,
                currentAnomaly = parseAnomaly(
                    currentAnomaly ?: error("AlertTicket must have currentAnomaly")
                )
            )
            "Acknowledged" -> AcknowledgedTicket(
                id = ticketId,
                version = aggregateVersion,
                userId = uid,
                flightIdentity = identity,
                acknowledgedAnomaly = parseAnomaly(
                    acknowledgedAnomaly ?: error("AcknowledgedTicket must have acknowledgedAnomaly")
                )
            )
            "Finished" -> FinishedTicket(
                id = ticketId,
                version = aggregateVersion,
                userId = uid,
                flightIdentity = identity,
                reason = FinishReason.valueOf(reason ?: error("FinishedTicket must have reason"))
            )
            else -> error("Unknown ticket type: $type")
        }
    }

    companion object {
        fun fromDomain(ticket: Ticket): TicketSnapshotDynamoItem {
            return TicketSnapshotDynamoItem(
                id = ticket.id.asString(),
                version = ticket.version.value,
                userId = ticket.userId.value(),
                flightIdentity = ticket.flightIdentity.asString(),
                type = when (ticket) {
                    is NormalTicket -> "Normal"
                    is AlertTicket -> "Alert"
                    is AcknowledgedTicket -> "Acknowledged"
                    is FinishedTicket -> "Finished"
                },
                currentAnomaly = (ticket as? AlertTicket)?.currentAnomaly?.toSummary(),
                acknowledgedAnomaly = (ticket as? AcknowledgedTicket)?.acknowledgedAnomaly?.toSummary(),
                reason = (ticket as? FinishedTicket)?.reason?.name
            )
        }

        private fun parseAnomaly(summary: String): Anomaly {
            return when {
                summary == "CANCELED" -> AnomalyCanceled
                summary.startsWith("DELAYED:") -> AnomalyDelayed(summary.substringAfter("DELAYED:"))
                summary.startsWith("UNCERTAIN:") -> AnomalyUncertain(summary.substringAfter("UNCERTAIN:"))
                else -> error("Unknown anomaly format: $summary")
            }
        }
    }
}
