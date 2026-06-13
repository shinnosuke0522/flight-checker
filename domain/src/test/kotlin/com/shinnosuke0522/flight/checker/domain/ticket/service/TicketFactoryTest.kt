package com.shinnosuke0522.flight.checker.domain.ticket.service

import arrow.core.Either
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.ticket.error.TicketAlreadyRegisteredError
import com.shinnosuke0522.flight.checker.domain.ticket.model.NormalTicket
import com.shinnosuke0522.flight.checker.domain.ticket.model.UserId
import com.shinnosuke0522.flight.checker.domain.ticket.policy.TicketDuplicatePolicy
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Instant
import java.time.LocalDate

class TicketFactoryTest : DescribeSpec({
    val userId = UserId.generate()
    val flightIdentity = FlightIdentity.create("JL123", LocalDate.of(2026, 6, 7)).getOrNull()!!
    val now = Instant.now()

    describe("Given: まだ登録されていないチケットを登録しようとする場合") {
        // 重複なしと判定されるPolicyを注入
        val policy = TicketDuplicatePolicy { _, _ -> false }
        val handler = TicketFactory.makeRegisteredHandler(policy)

        context("When: チケット登録を実行すると") {
            val result = handler(userId, flightIdentity, now)

            it("Then: 正常に登録され、NormalTicketが生成されること") {
                result.shouldBeInstanceOf<Either.Right<*>>()
                val (ticket, event) = (result as Either.Right).value
                ticket.shouldBeInstanceOf<NormalTicket>()
                ticket.userId shouldBe userId
                ticket.flightIdentity shouldBe flightIdentity
                event.userId shouldBe userId
                event.flightIdentity shouldBe flightIdentity
            }
        }
    }

    describe("Given: すでに同じユーザー・フライトで登録済みのチケットがある場合") {
        // 重複ありと判定されるPolicyを注入
        val policy = TicketDuplicatePolicy { _, _ -> true }
        val handler = TicketFactory.makeRegisteredHandler(policy)

        context("When: チケット登録を実行すると") {
            val result = handler(userId, flightIdentity, now)

            it("Then: TicketAlreadyRegisteredError エラーが返却されること") {
                result.shouldBeInstanceOf<Either.Left<*>>()
                val error = (result as Either.Left).value
                error.shouldBeInstanceOf<TicketAlreadyRegisteredError>()
                error.userId shouldBe userId
                error.flightIdentity shouldBe flightIdentity
            }
        }
    }
})
