package com.shinnosuke0522.flight.checker.domain.travel.model

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.shinnosuke0522.flight.checker.domain.base.model.CannotBeBlankError
import com.shinnosuke0522.flight.checker.domain.base.model.InvalidValueError
import com.shinnosuke0522.flight.checker.domain.base.model.ValidationError
import com.shinnosuke0522.flight.checker.domain.base.model.toCause
import com.shinnosuke0522.flight.checker.domain.base.value.ULID

@JvmInline
value class TravelId private constructor(val value: ULID) : Comparable<TravelId> {
    override fun compareTo(other: TravelId) = value.compareTo(other.value)

    companion object {
        fun generate() = TravelId(ULID.generate())

        operator fun invoke(
            value: String,
        ): Either<ValidationError, TravelId> = either {
            ensure(value.isNotBlank()) {
                CannotBeBlankError(valueName = this.javaClass.simpleName)
            }
            TravelId(
                value = ULID.of(value).getOrElse {
                    raise(
                        InvalidValueError(
                            valueName = this.javaClass.simpleName,
                            value = value,
                            cause = it.toCause()
                        )
                    )
                }
            )
        }
    }
}