package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.ticket

import com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.config.DataDynamoDbTest
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.ticket.event.TicketRegistered
import com.shinnosuke0522.flight.checker.domain.ticket.model.Ticket
import com.shinnosuke0522.flight.checker.domain.ticket.model.TicketId
import com.shinnosuke0522.flight.checker.domain.ticket.model.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

@DataDynamoDbTest
class TicketRepositoryDynamoDbImplIntegrationTest : FunSpec() {

    @Autowired
    lateinit var repository: TicketRepositoryDynamoDbImpl

    init {
        extension(SpringExtension())

        val ticketId = TicketId.generate()
        val userId = UserId.generate()
        val flightIdentity = FlightIdentity.create(
            rawFlightCode = "JL123",
            departureDate = LocalDate.of(2026, 5, 1)
        ).getOrNull()!!

        context("save & findById") {
            test("新しく作成した Ticket が保存・再構築できること") {
                val event = TicketRegistered(
                    id = DomainEventId.generate(),
                    meta = DomainEventMeta.forRootEvent { java.time.Instant.now() },
                    aggregateId = ticketId,
                    sequenceNumber = 1L,
                    userId = userId,
                    flightIdentity = flightIdentity
                )
                val ticket = Ticket.replay(arrow.core.nonEmptyListOf(event))

                repository.save(event, ticket)

                val restored = repository.findById(ticketId)
                restored shouldBe ticket
            }

            test("存在しない AggregateId で検索した場合は null が返ること") {
                val notFoundId = TicketId.generate()
                val restored = repository.findById(notFoundId)
                restored shouldBe null
            }

            test("同一バージョンに対する保存は Optimistic locking で失敗すること") {
                val duplicateEvent = TicketRegistered(
                    id = DomainEventId.generate(),
                    meta = DomainEventMeta.forRootEvent { java.time.Instant.now() },
                    aggregateId = ticketId,
                    sequenceNumber = 1L, // 既に 1L は保存済み
                    userId = userId,
                    flightIdentity = flightIdentity
                )
                val ticket = Ticket.replay(arrow.core.nonEmptyListOf(duplicateEvent))

                shouldThrow<IllegalStateException> {
                    repository.save(duplicateEvent, ticket)
                }
            }
        }

        context("findByUserId") {
            test("指定した UserId に紐づく Ticket が取得できること") {
                val tickets = repository.findByUserId(userId)
                tickets.shouldHaveSize(1)
                tickets[0].id shouldBe ticketId
            }

            test("存在しない UserId で検索した場合は空リストが返ること") {
                val notFoundUserId = UserId.generate()
                val tickets = repository.findByUserId(notFoundUserId)
                tickets.shouldHaveSize(0)
            }
        }

        context("findByFlightIdentity") {
            test("指定した FlightIdentity に紐づく Ticket が取得できること") {
                val tickets = repository.findByFlightIdentity(flightIdentity)
                tickets.shouldHaveSize(1)
                tickets[0].id shouldBe ticketId
            }

            test("存在しない FlightIdentity で検索した場合は空リストが返ること") {
                val notFoundFlightIdentity = FlightIdentity.create("NH999", LocalDate.of(2026, 5, 1)).getOrNull()!!
                val tickets = repository.findByFlightIdentity(notFoundFlightIdentity)
                tickets.shouldHaveSize(0)
            }
        }
    }
}
