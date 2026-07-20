package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.testfixture.config

import com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.eventstore.EventStoreItem
import com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.flight.FlightInfoSnapshotDynamoItem
import com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.ticket.TicketSnapshotDynamoItem
import jakarta.annotation.PostConstruct
import org.springframework.boot.test.context.TestConfiguration
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException

@TestConfiguration
open class DynamoDbTableInitializerConfig(
    private val enhancedClient: DynamoDbEnhancedClient
) {
    @PostConstruct
    fun createTables() {
        createTableIfNotExists("ticket-snapshots", TicketSnapshotDynamoItem::class.java)
        createTableIfNotExists("flight_info", FlightInfoSnapshotDynamoItem::class.java)
        createTableIfNotExists("ticket-journals", EventStoreItem::class.java)
        createTableIfNotExists("flight_info_event", EventStoreItem::class.java)
    }

    private fun <T> createTableIfNotExists(tableName: String, beanClass: Class<T>) {
        val table = enhancedClient.table(tableName, TableSchema.fromBean(beanClass))
        try {
            table.describeTable()
        } catch (ignored: ResourceNotFoundException) {
            table.createTable()
        }
    }
}
