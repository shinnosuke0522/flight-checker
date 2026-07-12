package com.shinnosuke0522.flight.checker.domain.shared.primitive

import com.shinnosuke0522.flight.checker.domain.base.error.CannotBeBlankError
import com.shinnosuke0522.flight.checker.domain.base.error.InvalidFormatError
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.engine.names.WithDataTestName
import io.kotest.matchers.shouldBe

data class InvalidCountryCodeTestCase(
    val name: String,
    val invalidValue: String,
    val expectedErrorType: Class<*>
) : WithDataTestName {
    override fun dataTestName() = name
}

class CountryCodeTest : FunSpec({
    context("有効な国コードであればインスタンスが生成できること") {
        withData(
            "JP",
            "US",
            "GB"
        ) { validCode ->
            val result = CountryCode(validCode)
            result.isRight() shouldBe true
            result.getOrNull()?.value shouldBe validCode
        }
    }

    context("不正な国コードの場合はエラーになること") {
        withData(
            InvalidCountryCodeTestCase("空文字の場合", "", CannotBeBlankError::class.java),
            InvalidCountryCodeTestCase("空白の場合", "  ", CannotBeBlankError::class.java),
            InvalidCountryCodeTestCase("文字数が短すぎる場合", "J", InvalidFormatError::class.java),
            InvalidCountryCodeTestCase("文字数が長すぎる場合", "JPN", InvalidFormatError::class.java),
            InvalidCountryCodeTestCase("小文字が含まれている場合", "Jp", InvalidFormatError::class.java),
            InvalidCountryCodeTestCase("数字が含まれている場合", "J1", InvalidFormatError::class.java)
        ) { testCase ->
            val result = CountryCode(testCase.invalidValue)
            result.isLeft() shouldBe true
            result.leftOrNull()?.javaClass shouldBe testCase.expectedErrorType
        }
    }
})
