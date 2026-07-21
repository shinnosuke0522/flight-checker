package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.config

import com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.testfixture.config.EnableDynamoDbContainer
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * DynamoDB 関連のコンポーネントに限定したスライステスト用アノテーション。
 *
 * Spring Boot の全自動設定を無効化し、軽量かつ高速なテスト環境を構築します。
 * 以下の設定が自動的に適用されます：
 * - [EnableDynamoDbContainer]: Testcontainers による LocalStack (DynamoDB) の起動
 * - [JacksonAutoConfiguration]: DynamoDB アダプターで必要となる Jackson (JSON) 設定の有効化
 * - [ComponentScan]: `com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb` 配下の Bean の読み込み
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(SpringExtension::class)
@EnableConfigurationProperties(DynamoDBLocalProps::class)
@Import(
    DataDynamoDbTestConfig::class
)
@EnableDynamoDbContainer
@ComponentScan(
    basePackages = ["com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb"]
)
annotation class DataDynamoDbTest
