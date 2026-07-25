package com.shinnosuke0522.flight.checker.domain.base.model

import arrow.core.Either
import arrow.core.raise.either
import com.github.f4b6a3.ulid.Ulid
import com.github.f4b6a3.ulid.UlidCreator
import java.time.Instant

@JvmInline
value class ULID private constructor(private val value: Ulid) : Comparable<ULID> {

    fun toInstant(): Instant = value.instant

    fun value() = value.toString()

    override fun compareTo(other: ULID): Int = this.value.compareTo(other.value)

    companion object {
        fun generate(): ULID = ULID(UlidCreator.getUlid())

        operator fun invoke(value: String): Either<InvariantError, ULID> = either {
            try {
                Ulid.from(value).let { ULID(it) }
            } catch (e: IllegalArgumentException) {
                raise(
                    InvalidValueError(
                        valueName = "ULID",
                        value = value,
                        cause = e.toCause()
                    ),
                )
            }
        }
    }
}
