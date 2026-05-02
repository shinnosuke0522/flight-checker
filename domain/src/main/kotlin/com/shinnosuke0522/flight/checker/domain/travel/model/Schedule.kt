package com.shinnosuke0522.flight.checker.domain.travel.model

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.shinnosuke0522.flight.checker.domain.base.error.BusinessRuleError
import com.shinnosuke0522.flight.checker.domain.base.error.Error
import com.shinnosuke0522.flight.checker.domain.base.error.ValidationError
import java.time.LocalDate

sealed interface Schedule {
    val departureDate: LocalDate
}

data class OneWayTripSchedule(
    override val departureDate: LocalDate
) : Schedule

@ConsistentCopyVisibility
data class RoundTripSchedule private constructor(
    override val departureDate: LocalDate,
    val returnDate: LocalDate
) : Schedule {
    companion object {
        operator fun invoke(
            departureDate: LocalDate,
            returnDate: LocalDate
        ): Either<TravelValidationError, RoundTripSchedule> = either {
            ensure(departureDate.isBefore(returnDate)) {
                ReturnDateBeforeDepartureDateError
            }
            RoundTripSchedule(departureDate, returnDate)
        }
    }
}