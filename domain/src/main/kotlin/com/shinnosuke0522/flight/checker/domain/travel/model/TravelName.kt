package com.shinnosuke0522.flight.checker.domain.travel.model

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.shinnosuke0522.flight.checker.domain.base.error.CannotBeBlankError
import com.shinnosuke0522.flight.checker.domain.base.error.InvariantError
import com.shinnosuke0522.flight.checker.domain.base.error.TooLongError

@JvmInline
value class TravelName private constructor(val value: String) {
    companion object {
        const val MAX_LENGTH = 100

        operator fun invoke(
            value: String,
        ): Either<InvariantError, TravelName> = either {
            ensure(value.isNotBlank()) {
                CannotBeBlankError(this.javaClass.simpleName)
            }
            ensure(value.length <= MAX_LENGTH) {
                TooLongError(
                    valueName = this.javaClass.simpleName,
                    value = value,
                    maxLength = MAX_LENGTH,
                )
            }
            TravelName(value)
        }
    }
}
