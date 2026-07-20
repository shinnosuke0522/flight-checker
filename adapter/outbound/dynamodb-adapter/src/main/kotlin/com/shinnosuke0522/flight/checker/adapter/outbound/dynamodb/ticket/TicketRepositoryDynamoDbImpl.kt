package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.ticket

import arrow.core.nonEmptyListOf
import com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.eventstore.DynamoDbEventStore
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketEvent
import com.shinnosuke0522.flight.checker.domain.ticket.model.Ticket
import com.shinnosuke0522.flight.checker.domain.ticket.model.TicketId
import com.shinnosuke0522.flight.checker.domain.ticket.model.UserId
import com.shinnosuke0522.flight.checker.domain.ticket.repository.TicketRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException

@Repository
final class TicketRepositoryDynamoDbImpl(
    private val enhancedClient: DynamoDbEnhancedClient,
    eventPayloadCodec: TicketEventDynamoPayloadCodec,
) : TicketRepository {

    private val snapshotTable = enhancedClient.table(
        SNAPSHOT_TABLE_NAME,
        TableSchema.fromBean(TicketSnapshotDynamoItem::class.java)
    )

    private val eventStore: DynamoDbEventStore<TicketId, Ticket, TicketEvent> =
        DynamoDbEventStore(
            enhancedClient = enhancedClient,
            tableName = JOURNAL_TABLE_NAME,
            serialize = eventPayloadCodec.serialize(),
            deserialize = eventPayloadCodec.deserialize(),
        )

    override suspend fun findById(ticketId: TicketId): Ticket? = withContext(Dispatchers.IO) {
        val snapshotItem = snapshotTable.getItem { reqBuilder ->
            reqBuilder.key { keyBuilder ->
                keyBuilder.partitionValue(ticketId.asString())
            }
        }

        if (snapshotItem != null) {
            val snapshot = snapshotItem.toDomain()
            return@withContext replayAggregateFromSnapshot(snapshot)
        }

        val events = eventStore.load(aggregateId = ticketId)
        if (events.isEmpty()) return@withContext null

        @Suppress("SpreadOperator")
        Ticket.replay(nonEmptyListOf(events.first(), *events.drop(1).toTypedArray()))
    }

    override suspend fun findByUserId(userId: UserId): List<Ticket> = withContext(Dispatchers.IO) {
        val snapshots = querySnapshotTable(
            index = snapshotTable.index("UserIdIndex"),
            conditional = QueryConditional.keyEqualTo { keyBuilder ->
                keyBuilder.partitionValue(userId.value())
            }
        ).map { it.toDomain() }

        snapshots.map { snapshot ->
            replayAggregateFromSnapshot(snapshot)
        }
    }

    override suspend fun findByFlightIdentity(
        flightIdentity: FlightIdentity
    ): List<Ticket> = withContext(Dispatchers.IO) {
        val index = snapshotTable.index("FlightIdentityIndex")
        val key = Key.builder().partitionValue(flightIdentity.asString()).build()

        val snapshots = index.query(QueryConditional.keyEqualTo(key))
            .stream()
            .flatMap { it.items().stream() }
            .map { it.toDomain() }
            .toList()

        snapshots.map { snapshot ->
            replayAggregateFromSnapshot(snapshot)
        }
    }

    override suspend fun save(event: TicketEvent, snapshot: Ticket): Unit = withContext(Dispatchers.IO) {
        val shouldSnapshot = event.sequenceNumber == 1L || event.sequenceNumber % SNAPSHOT_INTERVAL == 0L

        if (shouldSnapshot) {
            val eventRequest = eventStore.createTransactAppendRequest(event)
            val snapshotRequest = TransactPutItemEnhancedRequest.builder(TicketSnapshotDynamoItem::class.java)
                .item(TicketSnapshotDynamoItem.fromDomain(snapshot))
                .build()

            val transactRequest = TransactWriteItemsEnhancedRequest.builder()
                .addPutItem(eventStore.table, eventRequest)
                .addPutItem(snapshotTable, snapshotRequest)
                .build()

            try {
                enhancedClient.transactWriteItems(transactRequest)
            } catch (e: TransactionCanceledException) {
                throw IllegalStateException(
                    "Transaction canceled. Optimistic locking failed for " +
                        "aggregateId: ${event.aggregateId.asString()}, " +
                        "sequenceNumber: ${event.sequenceNumber}",
                    e
                )
            }
        } else {
            eventStore.append(event)
        }
    }

    private suspend fun replayAggregateFromSnapshot(
        snapshot: Ticket,
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

    private suspend fun querySnapshotTable(
        index: DynamoDbIndex<TicketSnapshotDynamoItem>,
        conditional: QueryConditional
    ): List<TicketSnapshotDynamoItem> {
        return index
            .query(conditional)
            .stream()
            .flatMap { it.items().stream() }
            .toList()
    }

    companion object {
        private const val JOURNAL_TABLE_NAME = "ticket-journals"
        private const val SNAPSHOT_TABLE_NAME = "ticket-snapshots"
        private const val SNAPSHOT_INTERVAL = 10L
    }
}
