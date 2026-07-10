package com.shinnosuke0522.flight.checker.domain.ticket.service

import arrow.core.Either
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.ticket.error.TicketAlreadyRegisteredError
import com.shinnosuke0522.flight.checker.domain.ticket.model.NormalTicket
import com.shinnosuke0522.flight.checker.domain.ticket.model.UserId
import com.shinnosuke0522.flight.checker.domain.ticket.policy.TicketDuplicatePolicy
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Instant
import java.time.LocalDate

class TicketFactoryTest : FunSpec({
    test("まだ登録されていないチケットを登録しようとした場合、正常に登録されNormalTicketが生成されること") {
        // 重複なしと判定されるPolicyを注入
        val policy = TicketDuplicatePolicy { _, _ -> false }
        val handler = TicketFactory.makeRegisteredHandler(policy)

        val result = handler(userId, flightIdentity, now)

        result.shouldBeInstanceOf<Either.Right<*>>()
        val (ticket, event) = (result as Either.Right).value
        ticket.shouldBeInstanceOf<NormalTicket>()
        ticket.userId shouldBe userId
        ticket.flightIdentity shouldBe flightIdentity
        event.userId shouldBe userId
        event.flightIdentity shouldBe flightIdentity
    }

    test("すでに同じユーザー・フライトで登録済みのチケットがある場合、TicketAlreadyRegisteredErrorが返却されること") {
        // 重複ありと判定されるPolicyを注入
        val policy = TicketDuplicatePolicy { _, _ -> true }
        val handler = TicketFactory.makeRegisteredHandler(policy)

        val result = handler(userId, flightIdentity, now)

        result.shouldBeInstanceOf<Either.Left<*>>()
        val error = (result as Either.Left).value
        error.shouldBeInstanceOf<TicketAlreadyRegisteredError>()
        error.userId shouldBe userId
        error.flightIdentity shouldBe flightIdentity
    }
}) {
    companion object {
        val userId = UserId.generate()
        val flightIdentity = FlightIdentity.create("JL123", LocalDate.of(2026, 6, 7)).getOrNull()!!
        val now = Instant.now()
    }
}
