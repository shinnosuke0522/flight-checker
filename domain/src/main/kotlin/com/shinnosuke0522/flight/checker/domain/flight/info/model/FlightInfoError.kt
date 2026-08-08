package com.shinnosuke0522.flight.checker.domain.flight.info.model

import com.shinnosuke0522.flight.checker.domain.base.model.BusinessRuleError
import com.shinnosuke0522.flight.checker.domain.base.model.CompositeInvariantError
import com.shinnosuke0522.flight.checker.domain.base.model.DomainError
import com.shinnosuke0522.flight.checker.domain.base.model.Error
import com.shinnosuke0522.flight.checker.domain.base.model.InvariantError
import com.shinnosuke0522.flight.checker.domain.base.model.RemoteError
import com.shinnosuke0522.flight.checker.domain.base.model.toCause
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightIdentity

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

sealed interface FlightInfoGatewayError : RemoteError

data class FlightInfoNotExistError(
    val flightIdentity: FlightIdentity,
    override val cause: Error.Cause.ThrowableCause? = null
) : FlightInfoGatewayError {
    override val message: String = "Flight info does not exist: $flightIdentity"
}

data class FlightInfoCommunicationError(
    val exception: Throwable
) : FlightInfoGatewayError {
    override val message: String = "Flight API通信に失敗しました"
    override val cause: Error.Cause = exception.toCause()
}

data class FlightInfoInvalidDataError(
    val exception: Throwable
) : FlightInfoGatewayError {
    override val message: String = "Flight APIから取得したデータの形式が不正、またはマッピングに失敗しました"
    override val cause: Error.Cause = exception.toCause()
}
