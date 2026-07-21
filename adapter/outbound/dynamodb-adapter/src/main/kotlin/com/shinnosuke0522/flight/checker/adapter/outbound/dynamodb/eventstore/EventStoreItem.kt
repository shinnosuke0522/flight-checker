package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.eventstore

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey

@Suppress("DataClassShouldBeImmutable")
@DynamoDbBean
data class EventStoreItem(
    @get:DynamoDbPartitionKey
    var aggregateId: String = "",
    @get:DynamoDbSortKey
    var sequenceNumber: Long = 0L,
    var payload: String = ""
)
