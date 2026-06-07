package com.shinnosuke0522.flight.checker.domain.ticket.model

import arrow.core.Either
import arrow.core.raise.either
import com.shinnosuke0522.flight.checker.domain.base.error.InvariantError
import com.shinnosuke0522.flight.checker.domain.base.primitive.ULID

/**
 * ユーザーを一意識別するためのID。
 */
@ConsistentCopyVisibility
data class UserId private constructor(val value: ULID) {
    fun value(): String = value.value()

    companion object {
        fun generate(): UserId = UserId(ULID.generate())
        fun fromString(str: String): Either<InvariantError, UserId> = either {
            UserId(ULID(str).bind())
        }
    }
}
