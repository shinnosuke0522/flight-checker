package com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.flightinfo

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.api.FlightAPIClient
import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.config.AeroDataBoxAPIConstants
import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.flightinfo.mapper.AeroDataBoxFlightMapper
import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.model.FlightSearchByEnum
import com.shinnosuke0522.flight.checker.domain.base.model.toCause
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfo
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfoCommunicationError
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfoGateway
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfoGatewayError
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfoNotExistError
import com.shinnosuke0522.flight.checker.domain.flight.model.FlightIdentity
import io.quarkus.arc.properties.IfBuildProperty
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.openapitools.client.infrastructure.ClientException
import org.openapitools.client.infrastructure.ServerException
import java.io.IOException
import java.time.OffsetDateTime

private const val HTTP_STATUS_NOT_FOUND = 404

@ApplicationScoped
@IfBuildProperty(
    name = AeroDataBoxAPIConstants.PROPERTY_ENABLED,
    stringValue = "true"
)
class FlightInfoGatewayAeroDataBoxImpl(
    private val flightApiClient: FlightAPIClient
) : FlightInfoGateway {
    override suspend fun fetchFlightInfo(identity: FlightIdentity): Either<FlightInfoGatewayError, FlightInfo> =
        withContext(Dispatchers.IO) {
            either {
                val dateLocalStr = "${identity.departureDate}T00:00:00Z"
                val dateLocal = OffsetDateTime.parse(dateLocalStr)

                val flights = try {
                    flightApiClient.getFlightFlightOnSpecificDate(
                        searchBy = FlightSearchByEnum.Number,
                        searchParam = identity.flightCode.value,
                        dateLocal = dateLocal
                    )
                } catch (e: ClientException) {
                    if (e.statusCode == HTTP_STATUS_NOT_FOUND) {
                        FlightInfoNotExistError(
                            flightIdentity = identity,
                            cause = e.toCause()
                        ).left().bind()
                    }
                    FlightInfoCommunicationError(e).left().bind()
                } catch (e: ServerException) {
                    FlightInfoCommunicationError(e).left().bind()
                } catch (e: IOException) {
                    FlightInfoCommunicationError(e).left().bind()
                }

                val flight = flights.firstOrNull() ?: FlightInfoNotExistError(identity).left().bind()
                AeroDataBoxFlightMapper.toDomain(flight, identity).bind()
            }
        }
}
