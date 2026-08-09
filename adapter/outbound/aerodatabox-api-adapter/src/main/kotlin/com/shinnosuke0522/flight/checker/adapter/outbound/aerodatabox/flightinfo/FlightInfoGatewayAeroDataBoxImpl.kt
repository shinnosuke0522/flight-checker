package com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.flightinfo

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.api.FlightApiClient
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
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.WebApplicationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.io.IOException

private const val HTTP_STATUS_NOT_FOUND = 404

@ApplicationScoped
@IfBuildProperty(
    name = AeroDataBoxAPIConstants.PROPERTY_ENABLED,
    stringValue = "true"
)
class FlightInfoGatewayAeroDataBoxImpl(
    @RestClient private val flightApiClient: FlightApiClient
) : FlightInfoGateway {
    override suspend fun fetchFlightInfo(identity: FlightIdentity): Either<FlightInfoGatewayError, FlightInfo> {
        return withContext(Dispatchers.IO) {
            either {
                val flights = try {
                    flightApiClient.getFlightFlightOnSpecificDate(
                        searchBy = FlightSearchByEnum.Number,
                        searchParam = identity.flightCode.value,
                        dateLocal = identity.departureDate,
                        dateLocalRole = null,
                        withAircraftImage = false,
                        withLocation = false,
                        withFlightPlan = false
                    )
                } catch (e: WebApplicationException) {
                    if (e.response.status == HTTP_STATUS_NOT_FOUND) {
                        FlightInfoNotExistError(
                            flightIdentity = identity,
                            cause = e.toThrowable().toCause()
                        ).left().bind()
                    }
                    FlightInfoCommunicationError(e).left().bind()
                } catch (e: ProcessingException) {
                    FlightInfoCommunicationError(e).left().bind()
                } catch (e: IOException) {
                    FlightInfoCommunicationError(e).left().bind()
                }

                val flight = flights?.firstOrNull() ?: FlightInfoNotExistError(identity).left().bind()
                AeroDataBoxFlightMapper.toDomain(flight, identity).bind()
            }
        }
    }

    private fun WebApplicationException.toThrowable(): Throwable {
        return Throwable("Status: ${this.response.status}, Body: ${this.response.readEntity(String::class.java)}")
    }
}
