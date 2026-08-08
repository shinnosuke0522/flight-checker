package com.shinnosuke0522.flight.checker.adapter.outbound.firestore.ticket

import arrow.core.nonEmptyListOf
import com.shinnosuke0522.flight.checker.adapter.outbound.firestore.config.DataFirestoreTest
import com.shinnosuke0522.flight.checker.domain.base.model.CorrelationId
import com.shinnosuke0522.flight.checker.domain.base.model.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.model.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.Ticket
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.TicketId
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.TicketRegistered
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.UserId
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

@DataFirestoreTest
class TicketRepositoryFirestoreImplIntegrationTest : StringSpec() {

    @Autowired
    lateinit var ticketRepository: TicketRepositoryFirestoreImpl

    init {
        extension(SpringExtension())
        "should save and find ticket" {
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
}
