package com.shinnosuke0522.flight.checker.domain.shared.primitive

import com.shinnosuke0522.flight.checker.domain.base.error.CannotBeBlankError
import com.shinnosuke0522.flight.checker.domain.base.error.InvalidFormatError
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.engine.names.WithDataTestName
import io.kotest.matchers.shouldBe

data class InvalidFlightCodeTestCase(
    val name: String,
    val invalidValue: String,
    val expectedErrorType: Class<*>
) : WithDataTestName {
    override fun dataTestName() = name
}

class FlightCodeTest : FunSpec({
    context("有効なフライトコードであればインスタンスが生成できること") {
        withData(
            "JL123",
            "NH1234",
            "7G12",
            "JL123A"
        ) { validCode ->
            val result = FlightCode(validCode)
            result.isRight() shouldBe true
            result.getOrNull()?.value shouldBe validCode
        }
    }

    context("プロパティが正しく取得できること") {
        test("キャリアコードと便名が正しく分割される") {
            val code = FlightCode("JL123A").getOrNull()
            code?.carrierCode shouldBe "JL"
            code?.flightNumber shouldBe "123A"
        }
    }

    context("不正なフライトコードの場合はエラーになること") {
        withData(
            InvalidFlightCodeTestCase("空文字の場合", "", CannotBeBlankError::class.java),
            InvalidFlightCodeTestCase("空白の場合", "     ", CannotBeBlankError::class.java),
            InvalidFlightCodeTestCase("短すぎる場合", "J", InvalidFormatError::class.java),
            InvalidFlightCodeTestCase("便名がない場合", "JL", InvalidFormatError::class.java),
            InvalidFlightCodeTestCase("小文字が含まれている場合", "Jl123", InvalidFormatError::class.java),
            InvalidFlightCodeTestCase("不正な文字が含まれている場合", "JL-123", InvalidFormatError::class.java)
        ) { testCase ->
            val result = FlightCode(testCase.invalidValue)
            result.isLeft() shouldBe true
            result.leftOrNull()?.javaClass shouldBe testCase.expectedErrorType
        }
    }
})
