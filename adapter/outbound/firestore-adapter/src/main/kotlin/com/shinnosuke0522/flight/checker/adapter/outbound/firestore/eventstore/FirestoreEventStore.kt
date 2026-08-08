package com.shinnosuke0522.flight.checker.adapter.outbound.firestore.eventstore

import com.google.cloud.firestore.Firestore
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateId
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateRoot
import com.shinnosuke0522.flight.checker.domain.base.model.DomainEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirestoreEventStore<ID : AggregateId, AGGREGATE : AggregateRoot<ID>, EVENT : DomainEvent<ID>>(
    val firestore: Firestore,
    val collectionName: String,
    private val serialize: (EVENT) -> String,
    private val deserialize: (EventStoreDocument) -> EVENT
) {

    suspend fun load(aggregateId: ID): List<EVENT> = withContext(Dispatchers.IO) {
        val querySnapshot = firestore.collection(collectionName)
            .whereEqualTo("aggregateId", aggregateId.asString())
            .orderBy("sequenceNumber")
            .get()
            .get()

        querySnapshot.documents.map {
            val doc = it.toObject(EventStoreDocument::class.java)
            deserialize(doc)
        }
    }

    suspend fun loadSince(aggregateId: ID, sequenceNumber: Long): List<EVENT> = withContext(Dispatchers.IO) {
        val querySnapshot = firestore.collection(collectionName)
            .whereEqualTo("aggregateId", aggregateId.asString())
            .whereGreaterThan("sequenceNumber", sequenceNumber)
            .orderBy("sequenceNumber")
            .get()
            .get()

        querySnapshot.documents.map {
            val doc = it.toObject(EventStoreDocument::class.java)
            deserialize(doc)
        }
    }

    suspend fun append(event: EVENT): Unit = withContext(Dispatchers.IO) {
        val docId = "${event.aggregateId.asString()}_${event.sequenceNumber}"
        val docRef = firestore.collection(collectionName).document(docId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef).get()
            if (snapshot.exists()) {
                throw IllegalStateException(
                    "Event already exists. Optimistic locking failed for " +
                        "aggregateId: ${event.aggregateId.asString()}, " +
                        "sequenceNumber: ${event.sequenceNumber}"
                )
            }

            val item = EventStoreDocument(
                aggregateId = event.aggregateId.asString(),
                sequenceNumber = event.sequenceNumber,
                payload = serialize(event)
            )

            transaction.set(docRef, item)
            null
        }.get()
    }
}
