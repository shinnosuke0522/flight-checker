package com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.flightinfo.mapper

import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.model.CodeshareStatus
import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.model.DateTimeContract
import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.model.FlightAirportMovementContract
import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.model.FlightContract
import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.model.FlightStatus
import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.model.ListingAirportContract
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightIdentity
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.engine.names.WithDataTestName
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.LocalDate
import java.time.OffsetDateTime

data class MissingFieldTestCase(
    val name: String,
    val modifyContract: (FlightContract) -> FlightContract,
    val expectedErrorMessage: String
) : WithDataTestName {
    override fun dataTestName() = name
}

class AeroDataBoxFlightMapperTest : FunSpec({

    context("正常系: 有効なFlightContractの場合") {
        test("FlightInfoに変換できること") {
            val result = AeroDataBoxFlightMapper.toDomain(validFlightContract, validFlightIdentity)

            val flightInfo = result.shouldBeRight()
            flightInfo.id shouldBe validFlightIdentity
            flightInfo.departurePoint.airportCode.value shouldBe "HND"
            flightInfo.arrivalPoint.airportCode.value shouldBe "JFK"
            flightInfo.scheduledDepartureTime shouldBe java.time.Instant.parse("2026-05-01T10:00:00Z")
            flightInfo.scheduledArrivalTime shouldBe java.time.Instant.parse("2026-05-01T20:00:00Z")
        }
    }

    context("異常系: 必須項目が欠損している場合") {
        withData(
            MissingFieldTestCase(
                name = "Departure Timeがnullの場合",
                modifyContract = {
                    it.copy(
                        departure = it.departure.copy(scheduledTime = null)
                    )
                },
                expectedErrorMessage = "Scheduled departure time is missing"
            ),
            MissingFieldTestCase(
                name = "Arrival Timeがnullの場合",
                modifyContract = {
                    it.copy(
                        arrival = it.arrival.copy(scheduledTime = null)
                    )
                },
                expectedErrorMessage = "Scheduled arrival time is missing"
            ),
            MissingFieldTestCase(
                name = "Departure CountryCodeがnullの場合",
                modifyContract = {
                    it.copy(
                        departure = it.departure.copy(
                            airport = it.departure.airport.copy(countryCode = null)
                        )
                    )
                },
                expectedErrorMessage = "Departure country code is missing"
            ),
            MissingFieldTestCase(
                name = "Arrival CountryCodeがnullの場合",
                modifyContract = {
                    it.copy(
                        arrival = it.arrival.copy(
                            airport = it.arrival.airport.copy(countryCode = null)
                        )
                    )
                },
                expectedErrorMessage = "Arrival country code is missing"
            ),
            MissingFieldTestCase(
                name = "Departure TimeZoneがnullの場合",
                modifyContract = {
                    it.copy(
                        departure = it.departure.copy(
                            airport = it.departure.airport.copy(timeZone = null)
                        )
                    )
                },
                expectedErrorMessage = "Departure timezone is missing"
            ),
            MissingFieldTestCase(
                name = "Arrival TimeZoneがnullの場合",
                modifyContract = {
                    it.copy(
                        arrival = it.arrival.copy(
                            airport = it.arrival.airport.copy(timeZone = null)
                        )
                    )
                },
                expectedErrorMessage = "Arrival timezone is missing"
            ),
            MissingFieldTestCase(
                name = "Departure IATA/ICAOがともにnullの場合",
                modifyContract = {
                    it.copy(
                        departure = it.departure.copy(
                            airport = it.departure.airport.copy(iata = null, icao = null)
                        )
                    )
                },
                expectedErrorMessage = "Departure IATA/ICAO is missing"
            ),
            MissingFieldTestCase(
                name = "Arrival IATA/ICAOがともにnullの場合",
                modifyContract = {
                    it.copy(
                        arrival = it.arrival.copy(
                            airport = it.arrival.airport.copy(iata = null, icao = null)
                        )
                    )
                },
                expectedErrorMessage = "Arrival IATA/ICAO is missing"
            )
        ) { testCase ->
            val invalidContract = testCase.modifyContract(validFlightContract)
            val result = AeroDataBoxFlightMapper.toDomain(invalidContract, validFlightIdentity)

            val error = result.shouldBeLeft()
            error.exception.shouldBeInstanceOf<IllegalArgumentException>()
            error.exception.message shouldBe testCase.expectedErrorMessage
        }
    }

    context("異常系: ドメインの生成ルールに違反する場合") {
        test("DepartureとArrivalの空港が同じ場合、IllegalStateExceptionが返ること") {
            val invalidContract = validFlightContract.copy(
                arrival = validFlightContract.departure
            )
            val result = AeroDataBoxFlightMapper.toDomain(invalidContract, validFlightIdentity)

            val error = result.shouldBeLeft()
            error.exception.shouldBeInstanceOf<IllegalStateException>()
            error.exception.message shouldBe "The arrival airport must not be same as the departure airport"
        }
    }
}) {
    companion object {
        val validFlightIdentity = FlightIdentity.create("JL123", LocalDate.of(2026, 5, 1)).shouldBeRight()

        val validListingAirport = ListingAirportContract(
            name = "Tokyo Haneda",
            iata = "HND",
            icao = "RJTT",
            countryCode = "JP",
            timeZone = "Asia/Tokyo"
        )

        val validArrivalAirport = ListingAirportContract(
            name = "New York JFK",
            iata = "JFK",
            icao = "KJFK",
            countryCode = "US",
            timeZone = "America/New_York"
        )

        val validDepartureTime = DateTimeContract(
            utc = OffsetDateTime.parse("2026-05-01T10:00:00Z"),
            local = OffsetDateTime.parse("2026-05-01T19:00:00Z")
        )

        val validArrivalTime = DateTimeContract(
            utc = OffsetDateTime.parse("2026-05-01T20:00:00Z"),
            local = OffsetDateTime.parse("2026-05-01T16:00:00Z")
        )

        val validFlightContract = FlightContract(
            departure = FlightAirportMovementContract(
                airport = validListingAirport,
                quality = emptyList(),
                scheduledTime = validDepartureTime
            ),
            arrival = FlightAirportMovementContract(
                airport = validArrivalAirport,
                quality = emptyList(),
                scheduledTime = validArrivalTime
            ),
            lastUpdatedUtc = OffsetDateTime.parse("2026-05-01T09:00:00Z"),
            number = "JL123",
            status = FlightStatus.Expected,
            codeshareStatus = CodeshareStatus.IsOperator,
            isCargo = false
        )
    }
}
