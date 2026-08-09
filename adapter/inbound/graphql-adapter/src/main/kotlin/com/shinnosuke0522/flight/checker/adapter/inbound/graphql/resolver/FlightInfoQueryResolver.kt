package com.shinnosuke0522.flight.checker.adapter.inbound.graphql.resolver

import com.expediagroup.graphql.server.operations.Query
import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.directive.Size
import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.model.FlightIdentityInput
import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.model.FlightInfo

interface FlightInfoQueryResolver : Query {
    suspend fun flightInfo(identity: FlightIdentityInput): FlightInfo
    suspend fun flightInfos(@Size(max = 100) identities: List<FlightIdentityInput>): List<FlightInfo>
}
