package com.shinnosuke0522.flight.checker.common.aws.config

import io.smallrye.config.ConfigMapping
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.regions.Region

@ConditionalOnAwsEnabled
@ConfigMapping(prefix = AwsConfigConstants.PREFIX)
interface AwsProps {
    fun region(): String
    fun accessKey(): String
    fun secretKey(): String

    fun awsRegion(): Region = Region.of(region())
    fun credentials(): AwsCredentials = AwsBasicCredentials.create(accessKey(), secretKey())
}
