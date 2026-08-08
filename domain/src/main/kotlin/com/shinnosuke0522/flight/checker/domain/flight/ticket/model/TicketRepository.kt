package com.shinnosuke0522.flight.checker.domain.flight.ticket.model

import com.shinnosuke0522.flight.checker.domain.flight.model.FlightIdentity

interface TicketRepository {
    suspend fun findById(ticketId: TicketId): Ticket?
    suspend fun findByUserId(userId: UserId): List<Ticket>
    suspend fun findByFlightIdentity(flightIdentity: FlightIdentity): List<Ticket>
    suspend fun save(event: TicketEvent, snapshot: Ticket)
}
