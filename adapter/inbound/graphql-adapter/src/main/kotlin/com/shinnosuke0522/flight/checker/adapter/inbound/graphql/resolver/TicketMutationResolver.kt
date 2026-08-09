package com.shinnosuke0522.flight.checker.adapter.inbound.graphql.resolver

import com.expediagroup.graphql.server.operations.Mutation
import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.model.TicketRegisterInput
import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.model.TicketRegisterPayload
import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.model.TicketUnregisterInput
import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.model.TicketUnregisterPayload

interface TicketMutationResolver : Mutation {
    suspend fun registerFlightTicket(input: TicketRegisterInput): TicketRegisterPayload
    suspend fun unregisterFlightTicket(input: TicketUnregisterInput): TicketUnregisterPayload
}
