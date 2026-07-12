package com.shinnosuke0522.flight.checker.domain.ticket.repository

import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.ticket.model.Ticket
import com.shinnosuke0522.flight.checker.domain.ticket.model.TicketId
import com.shinnosuke0522.flight.checker.domain.ticket.model.UserId

interface TicketRepository {
    suspend fun findById(ticketId: TicketId): Ticket?
    suspend fun findByUserId(userId: UserId): List<Ticket>
    suspend fun findByFlightIdentity(flightIdentity: FlightIdentity): List<Ticket>
    suspend fun save(ticket: Ticket)
}
