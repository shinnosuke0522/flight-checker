package com.shinnosuke0522.flight.checker.domain.flight.info.model

import arrow.core.Either
import com.shinnosuke0522.flight.checker.domain.base.model.Error
import com.shinnosuke0522.flight.checker.domain.shared.model.FlightIdentity

interface FlightInfoGateway {
    suspend fun fetchFlightInfo(identity: FlightIdentity): Either<Error, FlightInfo>
}
