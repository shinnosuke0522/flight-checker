package com.shinnosuke0522.flight.checker.common.aws.config

import org.springframework.boot.context.properties.ConfigurationProperties
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.regions.Region

@ConditionalOnAwsEnabled
@ConfigurationProperties(prefix = "infrastructure.aws")
data class AwsProps(
    private val awsRegion: String,
    private val awsAccessKey: String,
    private val awsSecretKey: String,
) {
    val region: Region = Region.of(awsRegion)
    val credentials: AwsCredentials = AwsBasicCredentials.create(awsAccessKey, awsSecretKey)
}
