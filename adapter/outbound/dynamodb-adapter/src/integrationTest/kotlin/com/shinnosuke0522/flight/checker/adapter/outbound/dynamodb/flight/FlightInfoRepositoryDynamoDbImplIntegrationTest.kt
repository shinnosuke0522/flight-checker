package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.flight

import com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.config.DataDynamoDbTest
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.flight.event.FlightInfoRegistered
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightInfo
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightPoint
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

@DataDynamoDbTest
class FlightInfoRepositoryDynamoDbImplIntegrationTest : FunSpec() {

    @Autowired
    lateinit var repository: FlightInfoRepositoryDynamoDbImpl

    init {
        extension(SpringExtension())

        val flightIdentity = FlightIdentity.create("JL123", LocalDate.of(2026, 5, 1)).getOrNull()!!
        val departurePoint = FlightPoint.create("JP", "HND", "Asia/Tokyo").getOrNull()!!
        val arrivalPoint = FlightPoint.create("US", "JFK", "America/New_York").getOrNull()!!

        context("save & findByFlightIdentity") {
            test("新しく作成した FlightInfo が保存・再構築できること") {
                val event = FlightInfoRegistered(
                    id = DomainEventId.generate(),
                    meta = DomainEventMeta.forRootEvent { java.time.Instant.now() },
                    aggregateId = flightIdentity,
                    sequenceNumber = 1L,
                    departurePoint = departurePoint,
                    arrivalPoint = arrivalPoint,
                    scheduledDepartureTime = java.time.Instant.now(),
                    scheduledArrivalTime = java.time.Instant.now().plusSeconds(3600)
                )
                val flightInfo = FlightInfo.replay(arrow.core.nonEmptyListOf(event))

                repository.save(event, flightInfo)

                val restored = repository.findByFlightIdentity(flightIdentity)
                restored shouldBe flightInfo
            }

            test("存在しない AggregateId で検索した場合は null が返ること") {
                val notFoundIdentity = FlightIdentity.create("NH999", LocalDate.of(2026, 5, 1)).getOrNull()!!
                val restored = repository.findByFlightIdentity(notFoundIdentity)
                restored shouldBe null
            }

            test("同一バージョンに対する保存は Optimistic locking で失敗すること") {
                val duplicateEvent = FlightInfoRegistered(
                    id = DomainEventId.generate(),
                    meta = DomainEventMeta.forRootEvent { java.time.Instant.now() },
                    aggregateId = flightIdentity,
                    sequenceNumber = 1L, // 既に 1L は保存済み
                    departurePoint = departurePoint,
                    arrivalPoint = arrivalPoint,
                    scheduledDepartureTime = java.time.Instant.now(),
                    scheduledArrivalTime = java.time.Instant.now().plusSeconds(3600)
                )
                val flightInfo = FlightInfo.replay(arrow.core.nonEmptyListOf(duplicateEvent))

                shouldThrow<IllegalStateException> {
                    repository.save(duplicateEvent, flightInfo)
                }
            }
        }
    }
}
