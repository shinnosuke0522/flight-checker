package com.shinnosuke0522.flight.checker.domain.base.model

import com.shinnosuke0522.flight.checker.domain.base.event.DomainEvent
import java.io.Serializable

interface AggregateId : Serializable {
    fun asString(): String
}

@JvmInline
value class AggregateVersion(val value: Long) {
    fun nextVersion() = AggregateVersion(this.value + 1)

    companion object {
        // CREATED EVENT適用時に0にならなければならないので、-1としている
        const val INITIAL_VERSION = -1L

        operator fun invoke(): AggregateVersion = AggregateVersion(INITIAL_VERSION)
    }
}

interface AggregateRoot<ID : AggregateId> {
    val id: ID
    val version: AggregateVersion
}

interface EventSourcingAggregateRoot<
    ID : AggregateId,
    EVENT : DomainEvent<ID>,
    SELF : EventSourcingAggregateRoot<ID, EVENT, SELF>
    > : AggregateRoot<ID> {

    fun apply(event: EVENT): SELF
}
