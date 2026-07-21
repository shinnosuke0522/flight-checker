package com.shinnosuke0522.flight.checker.adapter.inbound.graphql.resolver

import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.model.FlightIdentityInput
import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.model.FlightInfo

interface FlightInfoQueryResolver {
    suspend fun flightInfo(identity: FlightIdentityInput): FlightInfo
    suspend fun flightInfos(identities: List<FlightIdentityInput>): List<FlightInfo>
}
