package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.config

import com.shinnosuke0522.flight.checker.common.aws.config.ConditionalOnAwsDisabled
import org.springframework.boot.context.properties.ConfigurationProperties

@ConditionalOnAwsDisabled
@ConfigurationProperties(prefix = "infrastructure.aws.dynamodb")
data class DynamoDBLocalProps(
    val endpoint: String
)
