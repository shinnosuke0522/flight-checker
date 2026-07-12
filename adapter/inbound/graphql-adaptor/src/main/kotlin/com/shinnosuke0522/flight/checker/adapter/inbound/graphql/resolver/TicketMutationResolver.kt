package com.shinnosuke0522.flight.checker.adapter.inbound.graphql.resolver

import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.model.TicketRegisterRequest
import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.model.TicketRegisterResponse
import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.model.TicketUnregisterRequest
import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.model.TicketUnregisterResponse

interface TicketMutationResolver {
    suspend fun registerFlightTicket(input: TicketRegisterRequest): TicketRegisterResponse
    suspend fun unregisterFlightTicket(input: TicketUnregisterRequest): TicketUnregisterResponse
}
