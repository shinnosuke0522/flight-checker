package com.shinnosuke0522.flight.checker.domain.base.primitive

import com.shinnosuke0522.flight.checker.domain.base.error.InvalidValueError
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ULIDTest : FunSpec({
    context("ULIDの生成とパース") {
        test("generateで新しいULIDが生成されること") {
            val ulid1 = ULID.generate()
            val ulid2 = ULID.generate()

            ulid1.value() shouldNotBe ulid2.value()
        }

        test("有効な文字列からULIDが生成できること") {
            val original = ULID.generate()
            val result = ULID(original.value())

            result.isRight() shouldBe true
            result.getOrNull()?.value() shouldBe original.value()
        }

        test("不正な文字列の場合はエラーになること") {
            val result = ULID("invalid-ulid")

            result.isLeft() shouldBe true
            result.leftOrNull()?.shouldBeInstanceOf<InvalidValueError>()
        }
    }
})
