package com.shinnosuke0522.flight.checker.adapter.inbound.graphql.model

import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.directive.Pattern
import com.shinnosuke0522.flight.checker.adapter.inbound.graphql.directive.Size
import java.time.LocalDate
import java.time.OffsetDateTime

// ==========================
// GraphQL Types (Outputs)
// ==========================

data class FlightInfo(
    val id: String,
    val identity: FlightIdentity,
    val departurePoint: FlightPoint,
    val arrivalPoint: FlightPoint,
    val scheduledDepartureTime: OffsetDateTime,
    val scheduledArrivalTime: OffsetDateTime,
    val monitoringStatus: MonitoringStatus,
    val flightStatus: FlightStatus,
    val estimatedDepartureTime: OffsetDateTime? = null,
    val estimatedArrivalTime: OffsetDateTime? = null,
    val reason: String? = null
)

data class FlightIdentity(
    val flightCode: String,
    val departureDate: LocalDate
)

data class FlightPoint(
    val countryCode: String,
    val airportCode: String,
    val zoneId: String
)

data class Ticket(
    val id: String,
    val userId: String,
    val flight: FlightInfo
)

enum class FlightStatus {
    SCHEDULED, DELAYED, ARRIVED, CANCELED, UNCERTAIN
}

enum class MonitoringStatus {
    IDLE, ACTIVATED, COMPLETED, FAILED
}

// ==========================
// GraphQL Inputs
// ==========================

data class FlightIdentityInput(
    @Pattern(regexp = "^([A-Z0-9]{2})(\\d{1,4}[A-Z]?)$")
    val flightCode: String,
    val departureDate: LocalDate
)

data class TicketFilterInput(
    @Size(max = 100)
    val ids: List<String>? = null,
    val durationFrom: LocalDate? = null,
    val durationEnd: LocalDate? = null
)

data class TicketRegisterInput(
    val flight: FlightIdentityInput
)

data class TicketUnregisterInput(
    val flight: FlightIdentityInput
)

// ==========================
// GraphQL Payloads
// ==========================

data class TicketRegisterPayload(
    val ticketId: String
)

data class TicketUnregisterPayload(
    val deletedTicketId: String
)
