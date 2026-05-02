package com.shinnosuke0522.flight.checker.domain.base.event

import arrow.core.Either
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateId
import com.shinnosuke0522.flight.checker.domain.base.error.ValidationError
import com.shinnosuke0522.flight.checker.domain.base.primitive.ULID
import java.time.Instant

interface DomainEvent<AGGREGATE_ID : AggregateId> {
    val id: DomainEventId
    val aggregateId: AGGREGATE_ID
    val sequenceNumber: Long
    val meta: DomainEventMeta
}

@JvmInline
value class DomainEventId private constructor(val value: ULID) : Comparable<DomainEventId> {
    override fun compareTo(other: DomainEventId): Int = value.compareTo(other.value)

    companion object {
        fun generate(): DomainEventId = DomainEventId(ULID.generate())

        operator fun invoke(value: String): Either<ValidationError, DomainEventId> =
            ULID(value).map { DomainEventId(it) }
    }
}

data class DomainEventMeta(
    val occurredAt: Instant,
    val correlationId: CorrelationId,
    val causationId: DomainEventId? = null
) {
    companion object Factory{
        fun forRootEvent(
            clock: () -> Instant,
        ): DomainEventMeta = DomainEventMeta(
            occurredAt = clock(),
            correlationId = CorrelationId.generate(),
        )

        fun forCausedEvent(
            clock: () -> Instant,
            correlationId: CorrelationId,
            causationId: DomainEventId,
        ): DomainEventMeta = DomainEventMeta(
            occurredAt = clock(),
            correlationId = correlationId,
            causationId = causationId,
        )
    }
}

@JvmInline
value class CorrelationId(val value: ULID) : Comparable<CorrelationId> {
    override fun compareTo(other: CorrelationId): Int = value.compareTo(other.value)

    companion object {
        fun generate(): CorrelationId = CorrelationId(ULID.generate())

        operator fun invoke(value: String): Either<ValidationError, CorrelationId> =
            ULID(value).map { CorrelationId(it) }
    }
}
