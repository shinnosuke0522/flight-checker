package com.shinnosuke0522.flight.checker.domain.flight.model

import com.shinnosuke0522.flight.checker.domain.base.error.InvalidFormatError
import com.shinnosuke0522.flight.checker.domain.base.error.UnKnownValueError
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

class FlightPointTest : FunSpec({
    context("FlightPointの生成と検証") {
        test("有効な値であればインスタンスが生成できること") {
            val result = FlightPoint.create("JP", "HND", "Asia/Tokyo")

            result.isRight() shouldBe true
            val point = result.getOrNull()
            point?.countryCode?.value shouldBe "JP"
            point?.airportCode?.value shouldBe "HND"
            point?.zoneId shouldBe ZoneId.of("Asia/Tokyo")
        }

        test("複数エラーがある場合は全て収集されること") {
            val result = FlightPoint.create("J", "HN", "Invalid/Zone")

            result.isLeft() shouldBe true
            val errors = result.leftOrNull()
            errors?.size shouldBe 3
            errors?.get(0)?.shouldBeInstanceOf<InvalidFormatError>() // CountryCode error
            errors?.get(1)?.shouldBeInstanceOf<InvalidFormatError>() // AirportCode error
            errors?.get(2)?.shouldBeInstanceOf<UnKnownValueError>() // ZoneId error
        }
    }

    context("ローカルタイムの変換") {
        test("Instantが正しいローカルタイムに変換されること") {
            val point = FlightPoint.create("JP", "HND", "Asia/Tokyo").getOrNull()!!
            val instant = Instant.parse("2026-05-01T01:00:00Z") // UTC 01:00 -> JST 10:00

            val localTime = point.localTime(instant)
            localTime shouldBe LocalTime.of(10, 0)
        }
    }
})
