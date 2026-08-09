package com.shinnosuke0522.flight.checker.adapter.outbound.firestore.ticket

import arrow.core.nonEmptyListOf
import com.google.cloud.firestore.Firestore
import com.shinnosuke0522.flight.checker.adapter.outbound.firestore.eventstore.EventStoreDocument
import com.shinnosuke0522.flight.checker.adapter.outbound.firestore.eventstore.FirestoreEventStore
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.Ticket
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.TicketEvent
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.TicketId
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.TicketRepository
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.UserId
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@ApplicationScoped
class TicketRepositoryFirestoreImpl(
    private val firestore: Firestore,
    private val eventPayloadCodec: TicketEventFirestorePayloadCodec,
) : TicketRepository {

    private val eventStore = FirestoreEventStore<TicketId, Ticket, TicketEvent>(
        firestore = firestore,
        collectionName = JOURNAL_COLLECTION_NAME,
        serialize = eventPayloadCodec.serialize(),
        deserialize = eventPayloadCodec.deserialize()
    )

    override suspend fun findById(id: TicketId): Ticket? =
        withContext(Dispatchers.IO) {
            val snapshotDoc = firestore.collection(SNAPSHOT_COLLECTION_NAME)
                .document(id.asString())
                .get()
                .get()

            if (snapshotDoc.exists()) {
                val snapshotItem = snapshotDoc.toObject(TicketSnapshotFirestoreDocument::class.java)
                if (snapshotItem != null) {
                    return@withContext replayAggregateFromSnapshot(snapshotItem.toDomain())
                }
            }

            val events = eventStore.load(id)
            if (events.isEmpty()) return@withContext null

            @Suppress("SpreadOperator")
            Ticket.replay(nonEmptyListOf(events.first(), *events.drop(1).toTypedArray()))
        }

    override suspend fun findByUserId(userId: UserId): List<Ticket> = withContext(
        Dispatchers.IO
    ) {
        val snapshotDocs = firestore.collection(SNAPSHOT_COLLECTION_NAME)
            .whereEqualTo("userId", userId.value())
            .get()
            .get()

        snapshotDocs.documents
            .mapNotNull { it.toObject(TicketSnapshotFirestoreDocument::class.java) }
            .map { snapshotItem -> replayAggregateFromSnapshot(snapshotItem.toDomain()) }
    }

    override suspend fun findByFlightIdentity(flightIdentity: FlightIdentity): List<Ticket> = withContext(
        Dispatchers.IO
    ) {
        val snapshotDocs = firestore.collection(SNAPSHOT_COLLECTION_NAME)
            .whereEqualTo("flightIdentity", flightIdentity.asString())
            .get()
            .get()

        snapshotDocs.documents
            .mapNotNull { it.toObject(TicketSnapshotFirestoreDocument::class.java) }
            .map { snapshotItem -> replayAggregateFromSnapshot(snapshotItem.toDomain()) }
    }

    override suspend fun save(event: TicketEvent, snapshot: Ticket): Unit =
        withContext(Dispatchers.IO) {
            val shouldSnapshot = event.sequenceNumber == 1L || event.sequenceNumber % SNAPSHOT_INTERVAL == 0L

            if (shouldSnapshot) {
                val eventDocId = "${event.aggregateId.asString()}_${event.sequenceNumber}"
                val eventRef = firestore.collection(JOURNAL_COLLECTION_NAME).document(eventDocId)
                val snapshotRef = firestore.collection(SNAPSHOT_COLLECTION_NAME).document(snapshot.id.asString())

                firestore.runTransaction { transaction ->
                    val eventSnapshot = transaction.get(eventRef).get()
                    if (eventSnapshot.exists()) {
                        throw IllegalStateException(
                            "Transaction canceled. Optimistic locking failed for " +
                                "aggregateId: ${event.aggregateId.asString()}, " +
                                "sequenceNumber: ${event.sequenceNumber}"
                        )
                    }

                    val eventItem = EventStoreDocument(
                        aggregateId = event.aggregateId.asString(),
                        sequenceNumber = event.sequenceNumber,
                        payload = eventPayloadCodec.serialize()(event)
                    )

                    transaction.set(eventRef, eventItem)
                    transaction.set(snapshotRef, TicketSnapshotFirestoreDocument.fromDomain(snapshot))
                    null
                }.get()
            } else {
                eventStore.append(event)
            }
        }

    private suspend fun replayAggregateFromSnapshot(
        snapshot: Ticket
    ): Ticket {
        val events = eventStore.loadSince(
            aggregateId = snapshot.id,
            sequenceNumber = snapshot.version.value
        )
        return if (events.isEmpty()) {
            snapshot
        } else {
            events.fold(snapshot) { acc, e -> acc.apply(e) }
        }
    }

    companion object {
        private const val JOURNAL_COLLECTION_NAME = "ticket-journals"
        private const val SNAPSHOT_COLLECTION_NAME = "ticket-snapshots"
        private const val SNAPSHOT_INTERVAL = 10L
    }
}
