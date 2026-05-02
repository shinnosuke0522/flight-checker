package com.shinnosuke0522.flight.checker.domain.shared.value

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.shinnosuke0522.flight.checker.domain.base.error.CannotBeBlankError
import com.shinnosuke0522.flight.checker.domain.base.error.InvalidFormatError
import com.shinnosuke0522.flight.checker.domain.base.error.ValidationError

@JvmInline
value class FlightCode private constructor(val value: String) {
    val carrierCode get() = regex.find(value)!!.groups[CARRIER_CODE_GROUP]!!.value
    val flightNumber get() = regex.find(value)!!.groups[FLIGHT_NUMBER_GROUP]!!.value

    companion object {
        private const val PATTERN = "^([A-Z0-9]{2})(\\d{1,4}[A-Z]?)$"
        private const val CARRIER_CODE_GROUP = 1
        private const val FLIGHT_NUMBER_GROUP = 2

        private val regex = Regex(PATTERN)

        operator fun invoke(
            value: String,
        ): Either<ValidationError, FlightCode> = either {
            ensure(value.isNotBlank()) {
                CannotBeBlankError(valueName = this.javaClass.simpleName)
            }
            ensure(regex.matches(value)) {
                InvalidFormatError(
                    valueName = this.javaClass.simpleName,
                    value = value,
                    regex = regex,
                )
            }
            FlightCode(value)
        }
    }
}