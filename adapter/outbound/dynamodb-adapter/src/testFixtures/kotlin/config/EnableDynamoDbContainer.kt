package config

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