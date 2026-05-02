package com.shinnosuke0522.flight.checker.domain.travel.model

import arrow.core.Either
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateId
import com.shinnosuke0522.flight.checker.domain.base.error.ValidationError
import com.shinnosuke0522.flight.checker.domain.base.primitive.ULID

@JvmInline
value class TravelId private constructor(val value: ULID) : AggregateId, Comparable<TravelId> {
    override fun asString() = value.value()

    override fun compareTo(other: TravelId) = value.compareTo(other.value)

    companion object {
        fun generate() = TravelId(ULID.generate())

        operator fun invoke(value: String): Either<ValidationError, TravelId> =
            ULID(value).map { TravelId(it) }
    }
}