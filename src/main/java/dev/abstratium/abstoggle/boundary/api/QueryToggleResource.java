package dev.abstratium.abstoggle.boundary.api;

import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.abstratium.abstoggle.Roles;
import dev.abstratium.abstoggle.dto.ClientContextEntry;
import dev.abstratium.abstoggle.dto.EvaluatorResponse;
import dev.abstratium.abstoggle.dto.QueryResponse;
import dev.abstratium.abstoggle.service.ToggleQueryService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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

    /**
     * Evaluate toggles against a client context dictionary.
     * Returns the resolved values for each toggle based on rule matching.
     */
    @POST
    @Path("/evaluate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.QUERY, Roles.USER})
    public EvaluatorResponse evaluateToggles(
            @QueryParam("stage") String stage,
            @QueryParam("context") String context,
            @QueryParam("nameFilter") String nameFilter,
            List<ClientContextEntry> clientContext) {

        if (stage == null || stage.trim().isEmpty()) {
            throw new IllegalArgumentException("Stage parameter is required");
        }

        if (clientContext == null) {
            clientContext = Collections.emptyList();
        }

        // Authenticated endpoint includes debug information
        return toggleQueryService.evaluateToggles(stage, context, nameFilter, clientContext, true);
    }

    /**
     * Evict a specific entry from the evaluator cache.
     */
    @DELETE
    @Path("/evaluate/cache")
    @RolesAllowed({Roles.QUERY, Roles.USER})
    public Response evictEvaluatorCache(
            @QueryParam("stage") String stage,
            @QueryParam("context") String context,
            @QueryParam("nameFilter") String nameFilter,
            List<ClientContextEntry> clientContext) {

        if (clientContext == null) {
            clientContext = Collections.emptyList();
        }

        toggleQueryService.evictFromEvaluatorCache(stage, context, nameFilter, clientContext);
        return Response.noContent().build();
    }
}
