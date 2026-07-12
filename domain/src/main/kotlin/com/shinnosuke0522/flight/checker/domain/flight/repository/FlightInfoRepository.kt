package com.shinnosuke0522.flight.checker.domain.flight.repository

import com.shinnosuke0522.flight.checker.domain.flight.event.FlightInfoEvent
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightInfo
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity

interface FlightInfoRepository {
    suspend fun findByFlightIdentity(flightIdentity: FlightIdentity): FlightInfo?
    suspend fun save(event: FlightInfoEvent, snapshot: FlightInfo)
}
