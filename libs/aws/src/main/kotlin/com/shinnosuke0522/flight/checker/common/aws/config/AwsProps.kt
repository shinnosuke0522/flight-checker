package com.shinnosuke0522.flight.checker.common.aws.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.Name
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.regions.Region

@ConditionalOnAwsEnabled
@ConfigurationProperties(prefix = AwsConfigConstants.PREFIX)
data class AwsProps(
    @Name("region") private val awsRegion: String,
    @Name("access-key") private val awsAccessKey: String,
    @Name("secret-key") private val awsSecretKey: String,
) {
    val region: Region = Region.of(awsRegion)
    val credentials: AwsCredentials = AwsBasicCredentials.create(awsAccessKey, awsSecretKey)
}
