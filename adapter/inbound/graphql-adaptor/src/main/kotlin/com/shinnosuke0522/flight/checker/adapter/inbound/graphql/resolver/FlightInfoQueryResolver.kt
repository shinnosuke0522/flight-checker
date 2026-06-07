package com.shinnosuke0522.flight.checker.adapter.inbound.graphql.resolver

import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.model.FlightIdentityInput
import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.model.FlightInfo

interface FlightInfoQueryResolver {
    suspend fun flightInfo(id: FlightIdentityInput): FlightInfo
    suspend fun flightInfos(ids: List<FlightIdentityInput>): List<FlightInfo>
}
