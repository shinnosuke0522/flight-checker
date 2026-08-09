package com.shinnosuke0522.flight.checker.adapter.inbound.graphql.resolver

import com.expediagroup.graphql.server.operations.Query
import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.directive.Pattern
import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.model.Ticket
import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.model.TicketFilterInput

interface TicketQueryResolver : Query {
    suspend fun ticket(@Pattern(regexp = "^[0-9ABCDEFGHJKMNPQRSTVWXYZ]{26}$") id: String): Ticket?
    suspend fun tickets(filter: TicketFilterInput): List<Ticket>
}
