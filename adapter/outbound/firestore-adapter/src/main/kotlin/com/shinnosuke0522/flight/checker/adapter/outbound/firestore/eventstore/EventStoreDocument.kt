package com.shinnosuke0522.flight.checker.adapter.outbound.firestore.eventstore

@Suppress("DataClassShouldBeImmutable")
data class EventStoreDocument(
    var aggregateId: String = "",
    var sequenceNumber: Long = 0L,
    var payload: String = ""
)
