package com.shinnosuke0522.flight.checker.adapter.inbound.graphql.resolver

import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.model.Travel

interface TravelQueryResolver {
    suspend fun travel(id: String): Travel
    suspend fun travels(ids: List<String>): List<Travel>
}
