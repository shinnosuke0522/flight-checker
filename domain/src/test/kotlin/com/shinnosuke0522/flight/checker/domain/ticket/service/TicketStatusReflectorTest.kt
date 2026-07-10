package com.shinnosuke0522.flight.checker.domain.ticket.service

import arrow.core.getOrElse
import com.shinnosuke0522.flight.checker.domain.base.event.CorrelationId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.ticket.error.TicketAlreadyFinishedError
import com.shinnosuke0522.flight.checker.domain.ticket.error.TicketAlreadyOnScheduleError
import com.shinnosuke0522.flight.checker.domain.ticket.error.TicketAnomalyAlreadyReflectedError
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketAnomalyRecovered
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFlightCanceled
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketFlightDelayed
import com.shinnosuke0522.flight.checker.domain.ticket.model.AcknowledgedTicket
import com.shinnosuke0522.flight.checker.domain.ticket.model.AlertTicket
import com.shinnosuke0522.flight.checker.domain.ticket.model.AnomalyDelayed
import com.shinnosuke0522.flight.checker.domain.ticket.model.FinishReason
import com.shinnosuke0522.flight.checker.domain.ticket.model.FinishedTicket
import com.shinnosuke0522.flight.checker.domain.ticket.model.Ticket
import com.shinnosuke0522.flight.checker.domain.ticket.model.TicketId
import com.shinnosuke0522.flight.checker.domain.ticket.model.UserId
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Instant
import java.time.LocalDate

class TicketStatusReflectorTest : FunSpec() {
    init {
        test("監視中の通常のチケット (NormalTicket) がフライト欠航の事実を受け取った場合、欠航検知イベントが発行されること") {
            val command = TicketFlightCanceledReflectCommand(
                occurredAt = now,
                correlationId = correlationId,
                causationId = causationId
            )
            val result = TicketStatusReflector.reflect(normalTicket, command)

            val (_, event) = result.shouldBeRight()
            event.shouldBeInstanceOf<TicketFlightCanceled>()
        }

        test("監視中の通常のチケット (NormalTicket) がフライト遅延の事実を受け取った場合、遅延検知イベントが発行されること") {
            val detail = AnomalyDelayed("2026-06-07T10:00:00Z")
            val command = TicketFlightDelayedReflectCommand(
                occurredAt = now,
                correlationId = correlationId,
                causationId = causationId,
                detail = detail
            )
            val result = TicketStatusReflector.reflect(normalTicket, command)

            val (_, event) = result.shouldBeRight()
            event.shouldBeInstanceOf<TicketFlightDelayed>()
            (event as TicketFlightDelayed).detail shouldBe detail
        }

        test("監視中の通常のチケット (NormalTicket) が予定通りであるという事実を受け取った場合、エラーが返されること") {
            val command = TicketOnScheduleReflectCommand(
                occurredAt = now,
                correlationId = correlationId,
                causationId = causationId
            )
            val result = TicketStatusReflector.reflect(normalTicket, command)

            result shouldBeLeft TicketAlreadyOnScheduleError(normalTicket.id)
        }

        test("異常検知済みのチケット (AlertTicket) が前回と同じ遅延状態であるという事実を受け取った場合、二重通知を防ぐエラーが返されること") {
            val command = TicketFlightDelayedReflectCommand(
                occurredAt = now,
                correlationId = correlationId,
                causationId = causationId,
                detail = initialAlertDetail
            )
            val result = TicketStatusReflector.reflect(alertTicket, command)

            result shouldBeLeft TicketAnomalyAlreadyReflectedError(alertTicket.id, initialAlertDetail)
        }

        test("異常検知済みのチケット (AlertTicket) が前回の遅延から時刻が更新された事実を受け取った場合、新しい遅延検知イベントが発行されること") {
            val newDetail = AnomalyDelayed("2026-06-07T11:00:00Z")
            val command = TicketFlightDelayedReflectCommand(
                occurredAt = now,
                correlationId = correlationId,
                causationId = causationId,
                detail = newDetail
            )
            val result = TicketStatusReflector.reflect(alertTicket, command)

            val (_, event) = result.shouldBeRight()
            event.shouldBeInstanceOf<TicketFlightDelayed>()
            (event as TicketFlightDelayed).detail shouldBe newDetail
        }

        test("異常承諾済みのチケット (AcknowledgedTicket) がさらに遅延が更新された事実を受け取った場合、再度遅延検知イベントが発行されること") {
            val newDelay = AnomalyDelayed("2026-06-07T11:00:00Z")
            val command = TicketFlightDelayedReflectCommand(
                occurredAt = now,
                correlationId = correlationId,
                causationId = causationId,
                detail = newDelay
            )
            val result = TicketStatusReflector.reflect(acknowledgedTicket, command)

            val (_, event) = result.shouldBeRight()
            event.shouldBeInstanceOf<TicketFlightDelayed>()
            (event as TicketFlightDelayed).detail shouldBe newDelay
        }

        test("異常承諾済みのチケット (AcknowledgedTicket) が前回と同じ遅延状態である事実を受け取った場合、リマインドを抑制するエラーが返されること") {
            val command = TicketFlightDelayedReflectCommand(
                occurredAt = now,
                correlationId = correlationId,
                causationId = causationId,
                detail = initialAlertDetail
            )
            val result = TicketStatusReflector.reflect(acknowledgedTicket, command)

            result shouldBeLeft TicketAnomalyAlreadyReflectedError(acknowledgedTicket.id, initialAlertDetail)
        }

        test("異常承諾済みのチケット (AcknowledgedTicket) が予定通りに復帰した事実を受け取った場合、復旧イベントが発行されること") {
            val command = TicketOnScheduleReflectCommand(
                occurredAt = now,
                correlationId = correlationId,
                causationId = causationId
            )
            val result = TicketStatusReflector.reflect(acknowledgedTicket, command)

            val (_, event) = result.shouldBeRight()
            event.shouldBeInstanceOf<TicketAnomalyRecovered>()
        }

        test("終了済みのチケット (FinishedTicket) が何らかの事実を受け取った場合、変更不可能であることを示すエラーが返されること") {
            val finishedTicket = FinishedTicket(
                id = ticketId,
                version = AggregateVersion(2),
                userId = userId,
                flightIdentity = flightIdentity,
                reason = FinishReason.ARRIVED
            )
            val command = TicketOnScheduleReflectCommand(
                occurredAt = now,
                correlationId = correlationId,
                causationId = causationId
            )
            val result = TicketStatusReflector.reflect(finishedTicket, command)

            result shouldBeLeft TicketAlreadyFinishedError(finishedTicket.id)
        }
    }

    companion object {
        val userId = UserId.generate()
        val flightIdentity = FlightIdentity.create("JL123", LocalDate.of(2026, 6, 7)).getOrElse { error(it) }
        val ticketId = TicketId.generate()
        val now = Instant.now()
        val correlationId = CorrelationId.generate()
        val causationId = DomainEventId.generate()

        val normalTicket = Ticket.initial(ticketId, userId, flightIdentity)

        val initialAlertDetail = AnomalyDelayed("2026-06-07T10:00:00Z")
        val alertTicket = AlertTicket(
            id = ticketId,
            version = AggregateVersion(2),
            userId = userId,
            flightIdentity = flightIdentity,
            currentAnomaly = initialAlertDetail
        )

        val acknowledgedTicket = AcknowledgedTicket(
            id = ticketId,
            version = AggregateVersion(2),
            userId = userId,
            flightIdentity = flightIdentity,
            acknowledgedAnomaly = initialAlertDetail
        )
    }
}
