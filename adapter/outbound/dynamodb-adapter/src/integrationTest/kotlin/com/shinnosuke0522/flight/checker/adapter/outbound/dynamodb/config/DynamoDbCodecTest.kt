package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.config

import com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.flight.FlightInfoEventDynamoPayloadCodec
import com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.ticket.TicketEventDynamoPayloadCodec
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Codec (JSON シリアライズ/デシリアライズ) 専用のスライステスト用アノテーション。
 *
 * DynamoDB コンテナの起動を行わず、ObjectMapper と Codec コンポーネントのみを
 * ロードすることで、高速に JSON 変換処理をテストします。
 */

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(SpringExtension::class)
@Import(
    DataDynamoDbTestConfig::class,
    FlightInfoEventDynamoPayloadCodec::class,
    TicketEventDynamoPayloadCodec::class
)
annotation class DynamoDbCodecTest
