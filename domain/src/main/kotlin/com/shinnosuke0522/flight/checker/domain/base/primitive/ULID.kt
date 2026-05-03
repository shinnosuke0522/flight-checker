package com.shinnosuke0522.flight.checker.domain.base.primitive

import arrow.core.Either
import arrow.core.raise.either
import com.github.f4b6a3.ulid.Ulid
import com.github.f4b6a3.ulid.UlidCreator
import com.shinnosuke0522.flight.checker.domain.base.error.InvalidValueError
import com.shinnosuke0522.flight.checker.domain.base.error.ValidationError
import com.shinnosuke0522.flight.checker.domain.base.error.toCause
import java.time.Instant

@JvmInline
value class ULID private constructor(private val value: Ulid) : Comparable<ULID> {

    fun toInstant(): Instant = value.instant

    fun value() = value.toString()

    override fun compareTo(other: ULID): Int = this.value.compareTo(other.value)

    companion object {
        fun generate(): ULID = ULID(UlidCreator.getUlid())

        fun of(value: String) =
            runCatching { Ulid.from(value) }
                .map { ULID(it) }
                .recoverCatching {
                    throw IllegalArgumentException("Invalid ULID value: $value")
                }

        operator fun invoke(value: String): Either<ValidationError, ULID> = either {
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
