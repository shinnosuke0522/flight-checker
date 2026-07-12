package com.shinnosuke0522.flight.checker.domain.ticket.model

import com.shinnosuke0522.flight.checker.domain.base.error.InvalidValueError
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class TicketIdTest : FunSpec({
    context("TicketIdの生成とパース") {
        test("generateで新しいTicketIdが生成されること") {
            val ticketId = TicketId.generate()
            ticketId.asString().isNotBlank() shouldBe true
        }

        test("有効なULID文字列からTicketIdが生成できること") {
            val original = TicketId.generate()
            val result = TicketId.fromString(original.asString())

            result.isRight() shouldBe true
            result.getOrNull()?.asString() shouldBe original.asString()
        }

        test("不正な文字列の場合はエラーになること") {
            val result = TicketId.fromString("invalid-id")

            result.isLeft() shouldBe true
            result.leftOrNull()?.shouldBeInstanceOf<InvalidValueError>()
        }
    }
})
