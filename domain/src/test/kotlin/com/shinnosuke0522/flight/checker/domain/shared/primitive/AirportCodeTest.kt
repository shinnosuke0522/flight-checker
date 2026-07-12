package com.shinnosuke0522.flight.checker.domain.shared.primitive

import com.shinnosuke0522.flight.checker.domain.base.error.CannotBeBlankError
import com.shinnosuke0522.flight.checker.domain.base.error.InvalidFormatError
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.engine.names.WithDataTestName
import io.kotest.matchers.shouldBe

data class InvalidAirportCodeTestCase(
    val name: String,
    val invalidValue: String,
    val expectedErrorType: Class<*>
) : WithDataTestName {
    override fun dataTestName() = name
}

class AirportCodeTest : FunSpec({
    context("有効な空港コードであればインスタンスが生成できること") {
        withData(
            "HND",
            "JFK",
            "LHR"
        ) { validCode ->
            val result = AirportCode(validCode)
            result.isRight() shouldBe true
            result.getOrNull()?.value shouldBe validCode
        }
    }

    context("不正な空港コードの場合はエラーになること") {
        withData(
            InvalidAirportCodeTestCase("空文字の場合", "", CannotBeBlankError::class.java),
            InvalidAirportCodeTestCase("空白の場合", "   ", CannotBeBlankError::class.java),
            InvalidAirportCodeTestCase("文字数が短すぎる場合", "HN", InvalidFormatError::class.java),
            InvalidAirportCodeTestCase("文字数が長すぎる場合", "HNDD", InvalidFormatError::class.java),
            InvalidAirportCodeTestCase("小文字が含まれている場合", "Hnd", InvalidFormatError::class.java),
            InvalidAirportCodeTestCase("数字が含まれている場合", "HN1", InvalidFormatError::class.java)
        ) { testCase ->
            val result = AirportCode(testCase.invalidValue)
            result.isLeft() shouldBe true
            result.leftOrNull()?.javaClass shouldBe testCase.expectedErrorType
        }
    }
})
