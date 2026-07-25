package com.shinnosuke0522.flight.checker.domain.flight.gateway

import arrow.core.Either
import com.shinnosuke0522.flight.checker.domain.base.error.Error
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightInfo
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity

interface FlightInfoGateway {
    suspend fun fetchFlightInfo(identity: FlightIdentity): Either<Error, FlightInfo>
}
