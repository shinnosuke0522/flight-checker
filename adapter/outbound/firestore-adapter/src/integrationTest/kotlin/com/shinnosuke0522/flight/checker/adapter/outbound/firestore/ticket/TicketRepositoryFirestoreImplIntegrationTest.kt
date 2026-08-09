package com.shinnosuke0522.flight.checker.adapter.outbound.firestore.ticket

import arrow.core.nonEmptyListOf
import com.shinnosuke0522.flight.checker.domain.base.model.CorrelationId
import com.shinnosuke0522.flight.checker.domain.base.model.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.model.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.Ticket
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.TicketId
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.TicketRegistered
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.UserId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Instant

@QuarkusTest
class TicketRepositoryFirestoreImplIntegrationTest {

    @Inject
    lateinit var ticketRepository: TicketRepositoryFirestoreImpl

    @Test
    fun `should save and find ticket`() = runBlocking<Unit> {
        val ticketId = TicketId.generate()
        val userId = UserId.generate()
        val flightIdentity = FlightIdentity.create("NH123", java.time.LocalDate.of(2023, 12, 1)).getOrNull()!!

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
