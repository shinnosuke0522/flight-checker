package com.shinnosuke0522.flight.checker.domain.ticket.error

import com.shinnosuke0522.flight.checker.domain.base.error.BusinessRuleError
import com.shinnosuke0522.flight.checker.domain.base.error.DomainError
import com.shinnosuke0522.flight.checker.domain.base.error.Error
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.ticket.model.Anomaly
import com.shinnosuke0522.flight.checker.domain.ticket.model.TicketId
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

/**
 * すでに終了しているチケットに対して操作（状態更新）を行おうとした際のエラー。
 */
data class TicketAlreadyFinishedError(
    val ticketId: TicketId
) : TicketBusinessRuleError {
    override val cause: Error.Cause? = null
    override val message: String =
        "Cannot modify ticket ${ticketId.asString()} because it is already in a finished state."
}

/**
 * すでに同じ内容の異常が反映済みである際のエラー。
 * 同一の異常に対する重複通知を避けるために使用される。
 */
data class TicketAnomalyAlreadyReflectedError(
    val ticketId: TicketId,
    val detail: Anomaly
) : TicketBusinessRuleError {
    override val cause: Error.Cause? = null
    override val message: String =
        "Anomaly for ticket ${ticketId.asString()} is already reflected with: ${detail.toSummary()}"
}

/**
 * すでに予定通り（正常）な状態であるチケットに対して、再度正常であることを反映しようとした際のエラー。
 */
data class TicketAlreadyOnScheduleError(
    val ticketId: TicketId
) : TicketBusinessRuleError {
    override val cause: Error.Cause? = null
    override val message: String = "Ticket ${ticketId.asString()} is already on schedule."
}

data class TicketNotAlertStateError(
    val ticketId: TicketId
) : TicketBusinessRuleError {
    override val cause: Error.Cause? = null
    override val message: String = "Ticket ${ticketId.asString()} is not in an alert state and cannot be acknowledged."
}
