package com.shinnosuke0522.flight.checker.adapter.inbound.graphql.resolver

import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.model.Ticket
import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.model.TicketFilter

interface TicketQueryResolver {
    suspend fun ticket(id: String): Ticket
    suspend fun tickets(filter: TicketFilter): List<Ticket>
}
