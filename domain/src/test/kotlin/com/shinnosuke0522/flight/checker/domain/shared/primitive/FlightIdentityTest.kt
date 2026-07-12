package com.shinnosuke0522.flight.checker.domain.shared.primitive

import com.shinnosuke0522.flight.checker.domain.base.error.InvalidFormatError
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.LocalDate

class FlightIdentityTest : FunSpec({
    context("FlightIdentityの生成と検証") {
        test("有効な値であればインスタンスが生成できること") {
            val date = LocalDate.of(2026, 5, 1)
            val result = FlightIdentity.create("JL123", date)

            result.isRight() shouldBe true
            val identity = result.getOrNull()
            identity?.flightCode?.value shouldBe "JL123"
            identity?.departureDate shouldBe date
            identity?.asString() shouldBe "JL123-2026-05-01"
        }

        test("不正なフライトコードの場合はエラーになること") {
            val date = LocalDate.of(2026, 5, 1)
            val result = FlightIdentity.create("J-123", date) // 不正なフライトコード

            result.isLeft() shouldBe true
            result.leftOrNull()?.shouldBeInstanceOf<InvalidFormatError>()
        }
    }
})
