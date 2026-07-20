package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.testfixture.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration
final class DynamoDbContainerConfig {

    @Bean
    fun dynamoDbContainer(registry: DynamicPropertyRegistry): GenericContainer<*> {
        val container = GenericContainer(
            DockerImageName.parse(DYNAMODB_CONTAINER_IMAGE_NAME)
        ).withExposedPorts(DYNAMODB_CONTAINER_PORT)

        registry.add("infrastructure.aws.dynamodb.endpoint") {
            "http://${container.host}:${container.firstMappedPort}"
        }
        return container
    }

    companion object {
        private const val DYNAMODB_CONTAINER_IMAGE_NAME = "amazon/dynamodb-local:3.3.0"
        const val DYNAMODB_CONTAINER_PORT = 8000
    }
}