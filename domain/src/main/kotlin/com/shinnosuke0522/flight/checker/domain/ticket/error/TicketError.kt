package com.shinnosuke0522.flight.checker.domain.ticket.error

import com.shinnosuke0522.flight.checker.domain.base.error.BusinessRuleError
import com.shinnosuke0522.flight.checker.domain.base.error.DomainError
import com.shinnosuke0522.flight.checker.domain.base.error.Error
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.ticket.model.UserId

sealed interface TicketError : DomainError

sealed interface TicketBusinessRuleError : TicketError, BusinessRuleError

data class TicketAlreadyRegisteredError(
    val userId: UserId,
    val flightIdentity: FlightIdentity
) : TicketBusinessRuleError {
    override val cause: Error.Cause? = null
    override val message: String = "Ticket for flight $flightIdentity is already registered for user ${userId.value()}"
}
