package com.shinnosuke0522.flight.checker.domain.travel.error

import com.shinnosuke0522.flight.checker.domain.base.error.BusinessRuleError
import com.shinnosuke0522.flight.checker.domain.base.error.DomainError
import com.shinnosuke0522.flight.checker.domain.base.error.Error
import com.shinnosuke0522.flight.checker.domain.base.error.ValidationError
import com.shinnosuke0522.flight.checker.domain.travel.model.Schedule
import java.time.LocalDate

sealed interface TravelError : DomainError

sealed interface TravelValidationError : TravelError, ValidationError

data class InvalidTravelIdError(
    override val cause: Error.Cause.ErrorCause
) : TravelValidationError {
    override val message = "Invalid travel id"
}

data class InvalidTravelNameError(
    override val cause: Error.Cause.ErrorCause
) : TravelValidationError {
    override val message = "Invalid travel name"
}

data class InvalidFlightError(
    override val cause: Error.Cause.ErrorCause
) : TravelValidationError {
    override val message = "Invalid flight segment"
}

object ReturnDateBeforeDepartureDateError : TravelValidationError {
    override val cause: Error.Cause? = null
    override val message: String = "Return date must be after departure date"
}

data class FlightDateOutsideScheduleError(
    val flightDate: LocalDate,
    val schedule: Schedule
) : TravelValidationError {
    override val cause: Error.Cause? = null
    override val message: String = "Flight date $flightDate is outside of schedule $schedule"
}

object AtLeastOneFlightSegmentRequiredError : TravelBusinessRuleError {
    override val cause: Error.Cause? = null
    override val message: String = "At least one flight segment is required"
}

sealed interface TravelBusinessRuleError : TravelError, BusinessRuleError

object TravelAlreadyStartedError : TravelBusinessRuleError {
    override val cause: Error.Cause? = null
    override val message: String = "Travel has already started"
}

object TravelNotOngoingError : TravelBusinessRuleError {
    override val cause: Error.Cause? = null
    override val message: String = "Travel is not in ongoing status"
}


