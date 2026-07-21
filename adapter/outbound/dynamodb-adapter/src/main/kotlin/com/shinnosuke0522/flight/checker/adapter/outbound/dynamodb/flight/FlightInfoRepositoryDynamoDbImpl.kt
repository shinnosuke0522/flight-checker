package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.flight

import arrow.core.nonEmptyListOf
import com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.eventstore.DynamoDbEventStore
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightInfoEvent
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightInfo
import com.shinnosuke0522.flight.checker.domain.flight.repository.FlightInfoRepository
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException

@Repository
final class FlightInfoRepositoryDynamoDbImpl(
    private val enhancedClient: DynamoDbEnhancedClient,
    eventPayloadCodec: FlightInfoEventDynamoPayloadCodec,
) : FlightInfoRepository {

    private val snapshotTable = enhancedClient.table(
        SNAPSHOT_TABLE_NAME,
        TableSchema.fromBean(FlightInfoSnapshotDynamoItem::class.java)
    )

    private val eventStore: DynamoDbEventStore<FlightIdentity, FlightInfo, FlightInfoEvent> =
        DynamoDbEventStore(
            enhancedClient = enhancedClient,
            tableName = JOURNAL_TABLE_NAME,
            serialize = eventPayloadCodec.serialize(),
            deserialize = eventPayloadCodec.deserialize(),
        )

    override suspend fun findByFlightIdentity(flightIdentity: FlightIdentity): FlightInfo? =
        withContext(Dispatchers.IO) {
            val key = Key.builder().partitionValue(flightIdentity.asString()).build()
            val snapshotItem = snapshotTable.getItem(key)

            if (snapshotItem != null) {
                return@withContext replayAggregateFromSnapshot(snapshotItem.toDomain())
            }

            val events = eventStore.load(flightIdentity)
            if (events.isEmpty()) return@withContext null

            @Suppress("SpreadOperator")
            FlightInfo.replay(nonEmptyListOf(events.first(), *events.drop(1).toTypedArray()))
        }

    override suspend fun save(event: FlightInfoEvent, snapshot: FlightInfo): Unit =
        withContext(Dispatchers.IO) {
            val shouldSnapshot = event.sequenceNumber == 1L || event.sequenceNumber % SNAPSHOT_INTERVAL == 0L

            if (shouldSnapshot) {
                val eventRequest = eventStore.createTransactAppendRequest(event)
                val snapshotRequest = TransactPutItemEnhancedRequest.builder(FlightInfoSnapshotDynamoItem::class.java)
                    .item(FlightInfoSnapshotDynamoItem.fromDomain(snapshot))
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
        snapshot: FlightInfo
    ): FlightInfo {
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
        private const val JOURNAL_TABLE_NAME = "flight_info_event"
        private const val SNAPSHOT_TABLE_NAME = "flight_info"
        private const val SNAPSHOT_INTERVAL = 10L
    }
}
