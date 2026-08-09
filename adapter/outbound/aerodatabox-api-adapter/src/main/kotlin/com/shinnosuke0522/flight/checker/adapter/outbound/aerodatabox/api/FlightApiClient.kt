package com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.api

import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.model.FlightContract
import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.model.FlightDirection
import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.model.FlightSearchByEnum
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import java.time.LocalDate

@RegisterRestClient(configKey = "aerodatabox")
@ClientHeaderParam(name = "x-rapidapi-key", value = ["\${integration.external.api.aerodatabox.rapid-api-key}"])
@ClientHeaderParam(name = "x-rapidapi-host", value = ["\${integration.external.api.aerodatabox.rapid-api-host}"])
@ApplicationScoped
@Path("/flights")
interface FlightApiClient {

    @GET
    @Path("/{searchBy}/{searchParam}/{dateLocal}")
    @Produces("application/json", "application/xml")
    fun getFlightFlightOnSpecificDate(
        @PathParam("searchBy") searchBy: FlightSearchByEnum,
        @PathParam("searchParam") searchParam: String,
        @PathParam("dateLocal") dateLocal: LocalDate,
        @QueryParam("dateLocalRole") dateLocalRole: FlightDirection? = null,
        @QueryParam("withAircraftImage") @DefaultValue("false") withAircraftImage: Boolean? = false,
        @QueryParam("withLocation") @DefaultValue("false") withLocation: Boolean? = false,
        @QueryParam("withFlightPlan") @DefaultValue("false") withFlightPlan: Boolean? = false
    ): List<FlightContract>
}
