package com.shinnosuke0522.flight.checker.domain.flight.model

import com.shinnosuke0522.flight.checker.domain.base.error.BusinessRuleError
import com.shinnosuke0522.flight.checker.domain.base.error.DomainError
import com.shinnosuke0522.flight.checker.domain.base.error.Error
import com.shinnosuke0522.flight.checker.domain.base.error.InvariantViolationError
import com.shinnosuke0522.flight.checker.domain.base.error.ValidationError
import com.shinnosuke0522.flight.checker.domain.shared.value.FlightIdentity

interface FlightInfoError : DomainError

// Validation Error
sealed interface FlightInfoValidationError : FlightInfoError, ValidationError

data class InvalidFlightIdentityError(
    override val cause: Error.Cause.ErrorCause
) : FlightInfoValidationError {
    override val message = "Invalid flight identity"
}

data class InvalidDeparturePoint(
    override val cause: Error.Cause.ErrorCause
) : FlightInfoValidationError {
    override val message = "Invalid departure point"
}

data class InvalidArrivalPoint(
    override val cause: Error.Cause.ErrorCause
) : FlightInfoValidationError {
    override val message = "Invalid arrival point"
}

object SameFlightPointError
    : FlightInfoValidationError, InvariantViolationError {
    override val cause: Error.Cause? = null
    override val message: String =
        "The arrival airport must not be same as the departure airport"
}

object ScheduledArrivalTimeBeforeDepartureTimeError
    : FlightInfoValidationError, InvariantViolationError {
    override val cause: Error.Cause? = null
    override val message: String = "Scheduled arrival time must be after scheduled departure time"
}

object EstimatedArrivalTimeBeforeDepartureTimeError
    : FlightInfoValidationError, InvariantViolationError {
    override val cause: Error.Cause? = null
    override val message: String = "Estimated arrival time must be after estimated departure time"
}

// Business Rule
sealed interface FlightInfoBusinessRuleError: FlightInfoError, BusinessRuleError

data class FlightInfoAlreadyExists(
    val flightIdentity: FlightIdentity
): FlightInfoBusinessRuleError {
    override val cause: Error.Cause? = null
    override val message = "Flight info already exists: $flightIdentity"
}

data class FlightInfoNotFound(
    val flightIdentity: FlightIdentity
): FlightInfoBusinessRuleError {
    override val cause: Error.Cause? = null
    override val message = "Flight info not found: $flightIdentity"
}