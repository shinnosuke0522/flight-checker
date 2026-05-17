package com.shinnosuke0522.flight.checker.domain.shared.primitive

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.shinnosuke0522.flight.checker.domain.base.error.CannotBeBlankError
import com.shinnosuke0522.flight.checker.domain.base.error.InvalidFormatError
import com.shinnosuke0522.flight.checker.domain.base.error.InvariantError

/**
 * Represents an ISO 3166-1 alpha-2 country code.
 */
@JvmInline
value class CountryCode private constructor(val value: String) {
    companion object {
        private val regex = Regex("^[A-Z]{2}$")

        operator fun invoke(
            value: String,
        ): Either<InvariantError, CountryCode> = either {
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
            CountryCode(value = value)
        }
    }
}
