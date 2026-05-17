package com.shinnosuke0522.flight.checker.domain.travel.model

import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity

data class FlightSegment(
    val identity: FlightIdentity,
    val status: FlightSegmentStatus = FlightSegmentStatus.NORMAL,
) {
    internal fun updateStatus(newStatus: FlightSegmentStatus): FlightSegment = copy(status = newStatus)
}
