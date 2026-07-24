package com.shinnosuke0522.flight.checker.common.aws.config

import org.springframework.boot.context.properties.ConfigurationProperties
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.regions.Region

@ConditionalOnAwsEnabled
@ConfigurationProperties(prefix = AwsConfigConstants.PREFIX)
data class AwsProps(
    private val region: String,
    private val accessKey: String,
    private val secretKey: String,
) {
    val awsRegion: Region = Region.of(region)
    val credentials: AwsCredentials = AwsBasicCredentials.create(accessKey, secretKey)
}
