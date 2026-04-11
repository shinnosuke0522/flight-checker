package com.shinnosuke0522.flight.checker.domain.base.value

import com.github.f4b6a3.ulid.Ulid
import com.github.f4b6a3.ulid.UlidCreator
import com.github.f4b6a3.ulid.UlidFactory
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
    }
}