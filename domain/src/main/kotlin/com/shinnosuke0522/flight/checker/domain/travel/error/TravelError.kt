package com.shinnosuke0522.flight.checker.domain.travel.error

import com.shinnosuke0522.flight.checker.domain.base.error.BusinessRuleError
import com.shinnosuke0522.flight.checker.domain.base.error.DomainError
import com.shinnosuke0522.flight.checker.domain.base.error.Error
import com.shinnosuke0522.flight.checker.domain.base.error.InvariantError
import com.shinnosuke0522.flight.checker.domain.travel.model.Schedule
import com.shinnosuke0522.flight.checker.domain.travel.model.TravelId
import java.time.LocalDate

sealed interface TravelError : DomainError

sealed interface TravelInvariantError : TravelError, InvariantError

data class InvalidTravelIdError(
    override val cause: Error.Cause.ErrorCause
) : TravelInvariantError {
    override val message = "Invalid travel id"
}

data class InvalidTravelNameError(
    override val cause: Error.Cause.ErrorCause
) : TravelInvariantError {
    override val message = "Invalid travel name"
}

data class InvalidFlightError(
    override val cause: Error.Cause.ErrorCause
) : TravelInvariantError {
    override val message = "Invalid flight segment"
}

object ReturnDateBeforeDepartureDateError : TravelInvariantError {
    override val cause: Error.Cause? = null
    override val message: String = "Return date must be after departure date"
}

data class FlightDateOutsideScheduleError(
    val flightDate: LocalDate,
    val schedule: Schedule
) : TravelInvariantError {
    override val cause: Error.Cause? = null
    override val message: String = "Flight date $flightDate is outside of schedule $schedule"
}

object AtLeastOneFlightSegmentRequiredError : TravelBusinessRuleError {
    override val cause: Error.Cause? = null
    override val message: String = "At least one flight segment is required"
}

sealed interface TravelBusinessRuleError : TravelError, BusinessRuleError

data class TravelAlreadyStartedError(val id: TravelId) : TravelBusinessRuleError {
    override val cause: Error.Cause? = null
    override val message: String = "Travel $id has already been started"
}

data class TravelAlreadyCanceled(val id: TravelId) : TravelBusinessRuleError {
    override val cause: Error.Cause? = null
    override val message: String = "Travel $id has already been canceled"
}

data class TravelAlreadyCompleted(val id: TravelId) : TravelBusinessRuleError {
    override val cause: Error.Cause? = null
    override val message: String = "Travel $id has already been completed"
}

data class TravelNotStartedError(val id: TravelId) : TravelBusinessRuleError {
    override val cause: Error.Cause? = null
    override val message: String = "Travel has not been started"
}
