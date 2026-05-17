package dev.abstratium.abstoggle.boundary.api;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.abstratium.abstoggle.Roles;
import dev.abstratium.abstoggle.dto.QueryResponse;
import dev.abstratium.abstoggle.service.ToggleQueryService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/query/toggles")
@Tag(name = "Toggle Query", description = "Authenticated toggle query endpoint for server-to-server use")
public class QueryToggleResource {

    @Inject
    ToggleQueryService toggleQueryService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.QUERY, Roles.USER})
    public QueryResponse queryToggles(
            @QueryParam("stage") String stage,
            @QueryParam("context") String context,
            @QueryParam("nameFilter") String nameFilter,
            @QueryParam("includeDisabled") Boolean includeDisabled) {

        if (stage == null || stage.trim().isEmpty()) {
            throw new IllegalArgumentException("Stage parameter is required");
        }

        if (includeDisabled == null) {
            includeDisabled = false;
        }

        return toggleQueryService.queryToggles(stage, context, nameFilter, includeDisabled);
    }

    @DELETE
    @Path("/cache")
    @RolesAllowed({Roles.QUERY, Roles.USER})
    public Response evictCache(
            @QueryParam("stage") String stage,
            @QueryParam("context") String context,
            @QueryParam("nameFilter") String nameFilter,
            @QueryParam("includeDisabled") Boolean includeDisabled) {

        if (includeDisabled == null) {
            includeDisabled = false;
        }

        toggleQueryService.evictFromCache(stage, context, nameFilter, includeDisabled);
        return Response.noContent().build();
    }
}
