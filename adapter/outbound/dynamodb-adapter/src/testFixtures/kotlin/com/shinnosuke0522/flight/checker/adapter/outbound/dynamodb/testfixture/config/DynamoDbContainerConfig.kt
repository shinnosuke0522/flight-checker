package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.testfixture.config

import com.shinnosuke0522.flight.checker.common.aws.config.AwsConfigConstants
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistrar
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration
open class DynamoDbContainerConfig {

    @Bean
    open fun dynamoDbContainer(): GenericContainer<*> {
        return GenericContainer(DockerImageName.parse(DYNAMODB_CONTAINER_IMAGE_NAME))
            .withExposedPorts(DYNAMODB_CONTAINER_PORT)
    }

    @Bean
    open fun dynamoDbProperties(dynamoDbContainer: GenericContainer<*>): DynamicPropertyRegistrar {
        return DynamicPropertyRegistrar { registry ->
            registry.add("${AwsConfigConstants.PREFIX}.dynamodb.endpoint") {
                "http://${dynamoDbContainer.host}:${dynamoDbContainer.firstMappedPort}"
            }
        }
    }

    companion object {
        private const val DYNAMODB_CONTAINER_IMAGE_NAME = "amazon/dynamodb-local:3.3.0"
        const val DYNAMODB_CONTAINER_PORT = 8000
    }
}
