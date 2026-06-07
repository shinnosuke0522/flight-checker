package com.shinnosuke0522.flight.checker.domain.ticket.model

import arrow.core.Either
import arrow.core.raise.either
import com.shinnosuke0522.flight.checker.domain.base.error.InvariantError
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateId
import com.shinnosuke0522.flight.checker.domain.base.primitive.ULID

@ConsistentCopyVisibility
data class TicketId private constructor(val value: ULID) : AggregateId {
    override fun asString(): String = value.value()

    companion object {
        fun generate(): TicketId = TicketId(ULID.generate())
        fun fromString(str: String): Either<InvariantError, TicketId> = either {
            TicketId(ULID(str).bind())
        }
    }
}
