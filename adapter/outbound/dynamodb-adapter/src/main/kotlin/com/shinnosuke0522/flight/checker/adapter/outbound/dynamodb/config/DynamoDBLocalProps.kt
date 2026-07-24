package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.config

import com.shinnosuke0522.flight.checker.common.aws.config.AwsConfigConstants
import com.shinnosuke0522.flight.checker.common.aws.config.ConditionalOnAwsDisabled
import org.springframework.boot.context.properties.ConfigurationProperties

@ConditionalOnAwsDisabled
@ConfigurationProperties(prefix = "${AwsConfigConstants.PREFIX}.dynamodb")
data class DynamoDBLocalProps(
    val endpoint: String
)
