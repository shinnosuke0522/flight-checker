package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.config

import com.shinnosuke0522.flight.checker.common.aws.config.AwsProps
import com.shinnosuke0522.flight.checker.common.aws.config.ConditionalOnAwsDisabled
import com.shinnosuke0522.flight.checker.common.aws.config.ConditionalOnAwsEnabled
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.net.URI

@Configuration(proxyBeanMethods = false)
final class DynamoDbAdapterConfig {

    @Bean
    @ConditionalOnAwsEnabled
    fun dynamoDbClient(awsProps: AwsProps): DynamoDbClient =
        DynamoDbClient.builder()
            .region(awsProps.region)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    awsProps.credentials
                )
            )
            .build()

    @Bean
    @ConditionalOnAwsDisabled
    fun dynamoDbEnhancedClientForLocal(
        props: DynamoDBLocalProps
    ): DynamoDbClient =
        DynamoDbClient.builder()
            .endpointOverride(URI.create(props.endpoint))
            .region(Region.AP_NORTHEAST_1)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        "dummyAccessKey",
                        "dummySecretKey"
                    )
                )
            ).build()

    @Bean
    fun dynamoDbEnhancedClient(client: DynamoDbClient): DynamoDbEnhancedClient =
        DynamoDbEnhancedClient.builder()
            .dynamoDbClient(client)
            .build()
}
