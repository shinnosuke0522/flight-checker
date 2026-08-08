package com.shinnosuke0522.flight.checker.adapter.outbound.firestore.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.api.gax.core.CredentialsProvider
import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.rpc.HeaderProvider
import com.google.api.gax.rpc.NoHeaderProvider
import com.google.cloud.spring.core.GcpProjectIdProvider
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class DataFirestoreTestConfig {
    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper()

    @Bean
    fun headerProvider(): HeaderProvider = NoHeaderProvider()

    @Bean
    fun gcpProjectIdProvider(): GcpProjectIdProvider = GcpProjectIdProvider { "test-project" }

    @Bean
    fun googleCredentialsProvider(): CredentialsProvider = NoCredentialsProvider.create()
}
