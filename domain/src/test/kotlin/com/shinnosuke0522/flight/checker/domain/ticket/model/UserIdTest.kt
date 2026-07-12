package com.shinnosuke0522.flight.checker.domain.ticket.model

import com.shinnosuke0522.flight.checker.domain.base.error.InvalidValueError
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class UserIdTest : FunSpec({
    context("UserIdの生成とパース") {
        test("generateで新しいUserIdが生成されること") {
            val userId = UserId.generate()
            userId.value().isNotBlank() shouldBe true
        }

        test("有効なULID文字列からUserIdが生成できること") {
            val original = UserId.generate()
            val result = UserId.fromString(original.value())

            result.isRight() shouldBe true
            result.getOrNull()?.value() shouldBe original.value()
        }

        test("不正な文字列の場合はエラーになること") {
            val result = UserId.fromString("invalid-id")

            result.isLeft() shouldBe true
            result.leftOrNull()?.shouldBeInstanceOf<InvalidValueError>()
        }
    }
})
