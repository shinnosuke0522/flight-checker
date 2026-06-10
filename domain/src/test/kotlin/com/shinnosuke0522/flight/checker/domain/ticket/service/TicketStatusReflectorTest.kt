package com.shinnosuke0522.flight.checker.domain.ticket.service

import com.shinnosuke0522.flight.checker.domain.base.event.CorrelationId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.ticket.error.TicketAlreadyFinishedError
import com.shinnosuke0522.flight.checker.domain.ticket.error.TicketAlreadyOnScheduleError
import com.shinnosuke0522.flight.checker.domain.ticket.error.TicketAnomalyAlreadySynchronizedError
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
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Instant
import java.time.LocalDate

class TicketStatusReflectorTest : DescribeSpec({
    val userId = UserId.generate()
    val flightIdentity = FlightIdentity.create("JL123", LocalDate.of(2026, 6, 7)).shouldBeRight()
    val ticketId = TicketId.generate()
    val now = Instant.now()
    val correlationId = CorrelationId.generate()
    val causationId = DomainEventId.generate()

    describe("Given: 監視中の通常のチケット (NormalTicket) がある場合") {
        val ticket = Ticket.initial(ticketId, userId, flightIdentity)

        context("When: フライトが欠航したという事実を受け取った場合") {
            val command = TicketSyncFlightCanceledCommand(
                occurredAt = now,
                correlationId = correlationId,
                causationId = causationId
            )
            val result = TicketStatusReflector.reflect(ticket, command)

            it("Then: 欠航検知イベント (TicketFlightCanceled) が発行されること") {
                val (_, event) = result.shouldBeRight()
                event.shouldBeInstanceOf<TicketFlightCanceled>()
            }
        }

        context("When: フライトが遅延したという事実を受け取った場合") {
            val detail = AnomalyDelayed("2026-06-07T10:00:00Z")
            val command = TicketSyncFlightDelayedCommand(
                occurredAt = now,
                correlationId = correlationId,
                causationId = causationId,
                detail = detail
            )
            val result = TicketStatusReflector.reflect(ticket, command)

            it("Then: 遅延検知イベント (TicketFlightDelayed) が発行されること") {
                val (_, event) = result.shouldBeRight()
                event.shouldBeInstanceOf<TicketFlightDelayed>()
                (event as TicketFlightDelayed).detail shouldBe detail
            }
        }

        context("When: フライトが予定通り (OnSchedule) であるという事実を受け取った場合") {
            val command = TicketSyncOnScheduleCommand(
                occurredAt = now,
                correlationId = correlationId,
                causationId = causationId
            )
            val result = TicketStatusReflector.reflect(ticket, command)

            it("Then: すでに正常であるため、エラー (TicketAlreadyOnScheduleError) が返されること") {
                result shouldBeLeft TicketAlreadyOnScheduleError(ticket.id)
            }
        }
    }

    describe("Given: すでに異常を検知しているチケット (AlertTicket) がある場合") {
        val detail = AnomalyDelayed("2026-06-07T10:00:00Z")
        val ticket = AlertTicket(
            id = ticketId,
            version = AggregateVersion(2),
            userId = userId,
            flightIdentity = flightIdentity,
            currentAnomaly = detail
        )

        context("When: 前回と同じ遅延状態であるという事実を受け取った場合") {
            val command = TicketSyncFlightDelayedCommand(
                occurredAt = now,
                correlationId = correlationId,
                causationId = causationId,
                detail = detail
            )
            val result = TicketStatusReflector.reflect(ticket, command)

            it("Then: 二重通知を防ぐため、エラー (TicketAnomalyAlreadySynchronizedError) が返されること") {
                result shouldBeLeft TicketAnomalyAlreadySynchronizedError(ticket.id, detail)
            }
        }

        context("When: 前回の遅延から時刻が更新された事実を受け取った場合") {
            val newDetail = AnomalyDelayed("2026-06-07T11:00:00Z")
            val command = TicketSyncFlightDelayedCommand(
                occurredAt = now,
                correlationId = correlationId,
                causationId = causationId,
                detail = newDetail
            )
            val result = TicketStatusReflector.reflect(ticket, command)

            it("Then: 情報が更新されたため、新しい遅延検知イベントが発行されること") {
                val (_, event) = result.shouldBeRight()
                event.shouldBeInstanceOf<TicketFlightDelayed>()
                (event as TicketFlightDelayed).detail shouldBe newDetail
            }
        }
    }

    describe("Given: すでに異常を承諾済みのチケット (AcknowledgedTicket) がある場合") {
        val initialDetail = AnomalyDelayed("2026-06-07T10:00:00Z")
        val ticket = AcknowledgedTicket(
            id = ticketId,
            version = AggregateVersion(2),
            userId = userId,
            flightIdentity = flightIdentity,
            acknowledgedAnomaly = initialDetail
        )

        context("When: さらに遅延が更新されたという事実を受け取った場合") {
            val newDelay = AnomalyDelayed("2026-06-07T11:00:00Z")
            val command = TicketSyncFlightDelayedCommand(
                occurredAt = now,
                correlationId = correlationId,
                causationId = causationId,
                detail = newDelay
            )
            val result = TicketStatusReflector.reflect(ticket, command)

            it("Then: 内容が更新されたため、再度遅延検知イベント (TicketFlightDelayed) が発行されること") {
                val (_, event) = result.shouldBeRight()
                event.shouldBeInstanceOf<TicketFlightDelayed>()
                (event as TicketFlightDelayed).detail shouldBe newDelay
            }
        }

        context("When: 前回と同じ遅延状態であるという事実を受け取った場合") {
            val command = TicketSyncFlightDelayedCommand(
                occurredAt = now,
                correlationId = correlationId,
                causationId = causationId,
                detail = initialDetail
            )
            val result = TicketStatusReflector.reflect(ticket, command)

            it("Then: リマインドを抑制するため、エラー (TicketAnomalyAlreadySynchronizedError) が返されること") {
                result shouldBeLeft TicketAnomalyAlreadySynchronizedError(ticket.id, initialDetail)
            }
        }

        context("When: 状態が予定通り (Normal) に復帰したという事実を受け取った場合") {
            val command = TicketSyncOnScheduleCommand(
                occurredAt = now,
                correlationId = correlationId,
                causationId = causationId
            )
            val result = TicketStatusReflector.reflect(ticket, command)

            it("Then: 復旧イベント (TicketAnomalyRecovered) が発行されること") {
                val (_, event) = result.shouldBeRight()
                event.shouldBeInstanceOf<TicketAnomalyRecovered>()
            }
        }
    }

    describe("Given: すでに終了したチケット (FinishedTicket) がある場合") {
        val ticket = FinishedTicket(
            id = ticketId,
            version = AggregateVersion(2),
            userId = userId,
            flightIdentity = flightIdentity,
            reason = FinishReason.ARRIVED
        )

        context("When: 何らかの事実を受け取り反映させようとすると") {
            val command = TicketSyncOnScheduleCommand(
                occurredAt = now,
                correlationId = correlationId,
                causationId = causationId
            )
            val result = TicketStatusReflector.reflect(ticket, command)

            it("Then: 変更不可能であることを示すエラー (TicketAlreadyFinishedError) が返されること") {
                result shouldBeLeft TicketAlreadyFinishedError(ticket.id)
            }
        }
    }
})
