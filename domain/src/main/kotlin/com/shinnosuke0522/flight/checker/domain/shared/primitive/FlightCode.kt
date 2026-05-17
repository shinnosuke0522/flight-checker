package com.shinnosuke0522.flight.checker.domain.shared.primitive

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.shinnosuke0522.flight.checker.domain.base.error.CannotBeBlankError
import com.shinnosuke0522.flight.checker.domain.base.error.InvalidFormatError
import com.shinnosuke0522.flight.checker.domain.base.error.InvariantError

@JvmInline
value class FlightCode private constructor(val value: String) {
    val carrierCode get() = requireNotNull(regex.matchEntire(value)).groupValues[CARRIER_CODE_GROUP]
    val flightNumber get() = requireNotNull(regex.matchEntire(value)).groupValues[FLIGHT_NUMBER_GROUP]

    companion object {
        private const val PATTERN = "^([A-Z0-9]{2})(\\d{1,4}[A-Z]?)$"
        private const val CARRIER_CODE_GROUP = 1
        private const val FLIGHT_NUMBER_GROUP = 2

        private val regex = Regex(PATTERN)

        operator fun invoke(
            value: String,
        ): Either<InvariantError, FlightCode> = either {
            ensure(value.isNotBlank()) {
                CannotBeBlankError(valueName = "FlightCode")
            }
            ensure(regex.matches(value)) {
                InvalidFormatError(
                    valueName = "FlightCode",
                    value = value,
                    regex = regex,
                )
            }
            FlightCode(value)
        }
    }
}
