package com.shinnosuke0522.flight.checker.domain.travel.model

import com.shinnosuke0522.flight.checker.domain.base.error.BusinessRuleError
import com.shinnosuke0522.flight.checker.domain.base.error.DomainError
import com.shinnosuke0522.flight.checker.domain.base.error.Error
import com.shinnosuke0522.flight.checker.domain.base.error.ValidationError
import java.time.LocalDate

sealed interface TravelError: DomainError

sealed interface TravelValidationError: TravelError, ValidationError

data class InvalidTravelIdError(
    override val cause: Error.Cause.ErrorCause
): TravelValidationError {
    override val message = "Invalid travel id"
}

data class InvalidTravelNameError(
    override val cause: Error.Cause.ErrorCause
): TravelValidationError {
    override val message = "Invalid travel name"
}

data class InvalidFlightError(
    override val cause: Error.Cause.ErrorCause
): TravelValidationError {
    override val message = "Invalid flight segment"
}

object ReturnDateBeforeDepartureDateError: TravelValidationError {
    override val cause: Error.Cause? = null
    override val message: String = "Return date must be after departure date"
}