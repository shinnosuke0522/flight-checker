package com.shinnosuke0522.flight.checker.domain.travel.service

import arrow.core.NonEmptyList
import java.time.Instant
import java.time.LocalDate

sealed interface TravelCommand

data class TravelPlaneCommand(
    val rawTravelName: String,
    val departureDate: LocalDate,
    val returnDate: LocalDate? = null,
    val rawFlightSegments: NonEmptyList<Pair<String, LocalDate>>,
    val createdAt: Instant
) : TravelCommand
