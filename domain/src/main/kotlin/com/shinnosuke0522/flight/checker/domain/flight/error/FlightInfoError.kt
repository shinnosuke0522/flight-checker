package com.shinnosuke0522.flight.checker.domain.flight.error

import com.shinnosuke0522.flight.checker.domain.base.error.BusinessRuleError
import com.shinnosuke0522.flight.checker.domain.base.error.CompositeInvariantError
import com.shinnosuke0522.flight.checker.domain.base.error.DomainError
import com.shinnosuke0522.flight.checker.domain.base.error.Error
import com.shinnosuke0522.flight.checker.domain.base.error.InvariantError
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity

interface FlightInfoError : DomainError

// Invariant Error
sealed interface FlightInfoInvariantError : FlightInfoError, InvariantError

data class InvalidFlightIdentityError(
    override val cause: Error.Cause.ErrorCause
) : FlightInfoInvariantError {
    override val message = "Invalid flight identity"
}

data class InvalidDeparturePoint(
    override val cause: Error.Cause.ErrorCause
) : FlightInfoInvariantError {
    override val message = "Invalid departure point"
}

data class InvalidArrivalPoint(
    override val cause: Error.Cause.ErrorCause
) : FlightInfoInvariantError {
    override val message = "Invalid arrival point"
}

object SameFlightPointError :
    FlightInfoInvariantError, CompositeInvariantError {
    override val cause: Error.Cause? = null
    override val message: String =
        "The arrival airport must not be same as the departure airport"
}

object ScheduledArrivalTimeBeforeDepartureTimeError :
    FlightInfoInvariantError, CompositeInvariantError {
    override val cause: Error.Cause? = null
    override val message: String = "Scheduled arrival time must be after scheduled departure time"
}

object EstimatedArrivalTimeBeforeDepartureTimeError :
    FlightInfoInvariantError, CompositeInvariantError {
    override val cause: Error.Cause? = null
    override val message: String = "Estimated arrival time must be after estimated departure time"
}

// Business Rule
sealed interface FlightInfoBusinessRuleError : FlightInfoError, BusinessRuleError

data class FlightInfoAlreadyExists(
    val flightIdentity: FlightIdentity
) : FlightInfoBusinessRuleError {
    override val cause: Error.Cause? = null
    override val message = "Flight info already exists: $flightIdentity"
}

data class FlightInfoNotFound(
    val flightIdentity: FlightIdentity
) : FlightInfoBusinessRuleError {
    override val cause: Error.Cause? = null
    override val message = "Flight info not found: $flightIdentity"
}

data class FlightInfoAlreadyFinishedError(
    val flightIdentity: FlightIdentity
) : FlightInfoBusinessRuleError {
    override val cause: Error.Cause? = null
    override val message =
        "Cannot modify flight info ${flightIdentity.asString()} because it is already in a finished state."
}

data class FlightInfoAlreadyOnScheduleError(
    val flightIdentity: FlightIdentity
) : FlightInfoBusinessRuleError {
    override val cause: Error.Cause? = null
    override val message = "Flight info ${flightIdentity.asString()} is already on schedule."
}

data class FlightMonitoringAlreadyActivatedError(
    val flightIdentity: FlightIdentity
) : FlightInfoBusinessRuleError {
    override val cause: Error.Cause? = null
    override val message = "Monitoring for flight ${flightIdentity.asString()} is already activated or finished."
}

data class FlightMonitoringNotActivatedError(
    val flightIdentity: FlightIdentity
) : FlightInfoBusinessRuleError {
    override val cause: Error.Cause? = null
    override val message = "Monitoring for flight ${flightIdentity.asString()} is not activated."
}
