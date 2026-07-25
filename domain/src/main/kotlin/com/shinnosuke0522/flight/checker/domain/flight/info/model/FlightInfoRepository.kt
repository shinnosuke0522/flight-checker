package com.shinnosuke0522.flight.checker.domain.flight.info.model

import com.shinnosuke0522.flight.checker.domain.shared.model.FlightIdentity

interface FlightInfoRepository {
    suspend fun findByFlightIdentity(flightIdentity: FlightIdentity): FlightInfo?
    suspend fun save(event: FlightInfoEvent, snapshot: FlightInfo)
}
