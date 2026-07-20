package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class DataDynamoDbTestConfig {
    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper()
}
