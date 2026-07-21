package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.eventstore

import com.shinnosuke0522.flight.checker.domain.base.event.DomainEvent
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateId
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.Expression
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException

class DynamoDbEventStore<ID : AggregateId, AGGREGATE : AggregateRoot<ID>, EVENT : DomainEvent<ID>>(
    enhancedClient: DynamoDbEnhancedClient,
    tableName: String,
    private val serialize: (EVENT) -> String,
    private val deserialize: (EventStoreItem) -> EVENT
) {
    val table: DynamoDbTable<EventStoreItem> =
        enhancedClient.table(tableName, TableSchema.fromBean(EventStoreItem::class.java))

    suspend fun load(aggregateId: ID): List<EVENT> = withContext(Dispatchers.IO) {
        val key = Key.builder().partitionValue(aggregateId.asString()).build()
        val queryConditional = QueryConditional.keyEqualTo(key)

        table.query(queryConditional)
            .items()
            .map { deserialize(it) }
            .toList()
    }

    suspend fun loadSince(aggregateId: ID, sequenceNumber: Long): List<EVENT> = withContext(Dispatchers.IO) {
        val key = Key.builder()
            .partitionValue(aggregateId.asString())
            .sortValue(sequenceNumber)
            .build()
        val queryConditional = QueryConditional.sortGreaterThan(key)

        table.query(queryConditional)
            .items()
            .map { deserialize(it) }
            .toList()
    }

    fun createAppendRequest(event: EVENT): PutItemEnhancedRequest<EventStoreItem> {
        val item = EventStoreItem(
            aggregateId = event.aggregateId.asString(),
            sequenceNumber = event.sequenceNumber,
            payload = serialize(event)
        )

        val conditionExpression = Expression.builder()
            .expression("attribute_not_exists(aggregateId) AND attribute_not_exists(sequenceNumber)")
            .build()

        return PutItemEnhancedRequest.builder(EventStoreItem::class.java)
            .item(item)
            .conditionExpression(conditionExpression)
            .build()
    }

    fun createTransactAppendRequest(event: EVENT): TransactPutItemEnhancedRequest<EventStoreItem> {
        val item = EventStoreItem(
            aggregateId = event.aggregateId.asString(),
            sequenceNumber = event.sequenceNumber,
            payload = serialize(event)
        )

        val conditionExpression = Expression.builder()
            .expression("attribute_not_exists(aggregateId) AND attribute_not_exists(sequenceNumber)")
            .build()

        return TransactPutItemEnhancedRequest.builder(EventStoreItem::class.java)
            .item(item)
            .conditionExpression(conditionExpression)
            .build()
    }

    suspend fun append(event: EVENT): Unit = withContext(Dispatchers.IO) {
        try {
            table.putItem(createAppendRequest(event))
        } catch (e: ConditionalCheckFailedException) {
            throw IllegalStateException(
                "Event already exists. Optimistic locking failed for " +
                    "aggregateId: ${event.aggregateId.asString()}, " +
                    "sequenceNumber: ${event.sequenceNumber}",
                e
            )
        }
    }
}
