package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.testfixture.config

import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(DynamoDbContainerConfig::class)
@TestPropertySource(
    properties = [
        "infrastracture.aws.enabled=false"
    ]
)
annotation class EnableDynamoDbContainer