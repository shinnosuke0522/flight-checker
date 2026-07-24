package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.testfixture.config

import com.shinnosuke0522.flight.checker.common.aws.config.AwsConfigConstants
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(DynamoDbContainerConfig::class, DynamoDbTableInitializerConfig::class)
@TestPropertySource(
    properties = [
        "${AwsConfigConstants.PROPERTY_ENABLED}=false"
    ]
)
annotation class EnableDynamoDbContainer
