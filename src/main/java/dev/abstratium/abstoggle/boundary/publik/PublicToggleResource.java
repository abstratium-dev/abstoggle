package dev.abstratium.abstoggle.boundary.publik;

import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.abstratium.abstoggle.dto.ClientContextEntry;
import dev.abstratium.abstoggle.dto.EvaluatorResponse;
import dev.abstratium.abstoggle.dto.QueryResponse;
import dev.abstratium.abstoggle.service.ToggleQueryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/public/toggles")
@Tag(name = "Public Toggles", description = "Public toggle endpoints (no authentication required)")
public class PublicToggleResource {

    @Inject
    ToggleQueryService toggleQueryService;

    @ConfigProperty(name = "toggle.public-api.enabled", defaultValue = "true")
    boolean publicApiEnabled;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public QueryResponse queryToggles(
            @QueryParam("stage") String stage,
            @QueryParam("context") String context,
            @QueryParam("nameFilter") String nameFilter,
            @QueryParam("includeDisabled") Boolean includeDisabled) {

        if (!publicApiEnabled) {
            throw new NotFoundException("Public toggle API is disabled on this instance");
        }

        if (stage == null || stage.trim().isEmpty()) {
            throw new IllegalArgumentException("Stage parameter is required");
        }

        if (context == null || context.trim().isEmpty()) {
            throw new IllegalArgumentException("Context parameter is required");
        }

        if (includeDisabled == null) {
            includeDisabled = false;
        }

        return toggleQueryService.queryToggles(stage, context, nameFilter, includeDisabled);
    }

    /**
     * Evaluate toggles against a client context dictionary.
     * Returns the resolved values for each toggle based on rule matching.
     */
    @POST
    @Path("/evaluate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public EvaluatorResponse evaluateToggles(
            @QueryParam("stage") String stage,
            @QueryParam("context") String context,
            @QueryParam("nameFilter") String nameFilter,
            List<ClientContextEntry> clientContext) {

        if (!publicApiEnabled) {
            throw new NotFoundException("Public toggle API is disabled on this instance");
        }

        if (stage == null || stage.trim().isEmpty()) {
            throw new IllegalArgumentException("Stage parameter is required");
        }

        if (context == null || context.trim().isEmpty()) {
            throw new IllegalArgumentException("Context parameter is required");
        }

        if (clientContext == null) {
            clientContext = Collections.emptyList();
        }

        // Public endpoint does not include debug information
        return toggleQueryService.evaluateToggles(stage, context, nameFilter, clientContext, false);
    }
}
