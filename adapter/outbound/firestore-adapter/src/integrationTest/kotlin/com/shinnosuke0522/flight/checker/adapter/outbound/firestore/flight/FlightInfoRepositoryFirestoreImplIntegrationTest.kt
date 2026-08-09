package com.shinnosuke0522.flight.checker.adapter.outbound.firestore.flight

import com.shinnosuke0522.flight.checker.domain.base.model.CorrelationId
import com.shinnosuke0522.flight.checker.domain.base.model.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.model.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfoRegistered
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightPoint
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

@QuarkusTest
class FlightInfoRepositoryFirestoreImplIntegrationTest {

    @Inject
    lateinit var flightInfoRepository: FlightInfoRepositoryFirestoreImpl

    @Test
    fun `should save and find flight info`() = runBlocking<Unit> {
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
