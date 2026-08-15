package com.shinnosuke0522.flight.checker.adapter.outbound.firestore.ticket

import arrow.core.nonEmptyListOf
import com.shinnosuke0522.flight.checker.adapter.outbound.firestore.config.TestKoinContext
import com.shinnosuke0522.flight.checker.domain.base.model.CorrelationId
import com.shinnosuke0522.flight.checker.domain.base.model.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.model.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.Ticket
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.TicketId
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.TicketRegistered
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.TicketRepository
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.UserId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.koin.test.KoinTest
import org.koin.test.inject
import java.time.Instant
import java.time.LocalDate

class TicketRepositoryFirestoreImplIntegrationTest : FunSpec(), KoinTest {

    private val ticketRepository: TicketRepository by inject()

    init {
        TestKoinContext.start()
        test("should save and find ticket") {
            val ticketId = TicketId.generate()
            val userId = UserId.generate()
            val flightIdentity = FlightIdentity.create("NH123", LocalDate.of(2023, 12, 1)).getOrNull()!!

            val event = TicketRegistered(
                id = DomainEventId.generate(),
                aggregateId = ticketId,
                sequenceNumber = 1L,
                meta = DomainEventMeta(occurredAt = Instant.now(), correlationId = CorrelationId.generate()),
                userId = userId,
                flightIdentity = flightIdentity
            )

            val snapshot = Ticket.replay(nonEmptyListOf(event))

            ticketRepository.save(event, snapshot)

            val found = ticketRepository.findById(ticketId)
            found shouldNotBe null
            found?.id shouldBe ticketId
            found?.version?.value shouldBe 1L
        }
    }
}
