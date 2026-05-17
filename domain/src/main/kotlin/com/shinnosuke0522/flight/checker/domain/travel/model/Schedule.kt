package com.shinnosuke0522.flight.checker.domain.travel.model

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.shinnosuke0522.flight.checker.domain.travel.error.ReturnDateBeforeDepartureDateError
import com.shinnosuke0522.flight.checker.domain.travel.error.TravelInvariantError
import java.time.LocalDate

sealed interface Schedule {
    val departureDate: LocalDate

    fun contains(date: LocalDate): Boolean
}

data class OneWayTripSchedule(
    override val departureDate: LocalDate
) : Schedule {
    override fun contains(date: LocalDate): Boolean =
        !date.isBefore(departureDate)
}

@ConsistentCopyVisibility
data class RoundTripSchedule private constructor(
    override val departureDate: LocalDate,
    val returnDate: LocalDate
) : Schedule {
    override fun contains(date: LocalDate): Boolean =
        !date.isBefore(departureDate) && !date.isAfter(returnDate)

    companion object {
        operator fun invoke(
            departureDate: LocalDate,
            returnDate: LocalDate
        ): Either<TravelInvariantError, RoundTripSchedule> = either {
            ensure(departureDate.isBefore(returnDate)) {
                ReturnDateBeforeDepartureDateError
            }
            RoundTripSchedule(departureDate, returnDate)
        }
    }
}
