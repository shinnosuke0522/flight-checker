package com.shinnosuke0522.flight.checker.adapter.outbound.firestore.flight

import com.shinnosuke0522.flight.checker.adapter.outbound.firestore.config.DataFirestoreTest
import com.shinnosuke0522.flight.checker.domain.base.model.CorrelationId
import com.shinnosuke0522.flight.checker.domain.base.model.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.model.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfoRegistered
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightPoint
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate

@DataFirestoreTest
class FlightInfoRepositoryFirestoreImplIntegrationTest : FunSpec() {

    @Autowired
    lateinit var flightInfoRepository: FlightInfoRepositoryFirestoreImpl

    init {
        extension(SpringExtension())
        test("should save and find flight info") {
            val flightIdentity = FlightIdentity.create("NH123", LocalDate.of(2023, 12, 1)).getOrNull()!!

            val event = FlightInfoRegistered(
                id = DomainEventId.generate(),
                aggregateId = flightIdentity,
                sequenceNumber = 1L,
                meta = DomainEventMeta(occurredAt = Instant.now(), correlationId = CorrelationId.generate()),
                departurePoint = FlightPoint.create("JP", "HND", "Asia/Tokyo").getOrNull()!!,
                arrivalPoint = FlightPoint.create("US", "JFK", "America/New_York").getOrNull()!!,
                scheduledDepartureTime = Instant.now(),
                scheduledArrivalTime = Instant.now().plusSeconds(3600)
            )

            val snapshot = com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfo.replay(
                arrow.core.nonEmptyListOf(event)
            )

            flightInfoRepository.save(event, snapshot)

            val found = flightInfoRepository.findByFlightIdentity(flightIdentity)
            found shouldNotBe null
            found?.id shouldBe flightIdentity
            found?.version?.value shouldBe 1L
        }
    }
}
