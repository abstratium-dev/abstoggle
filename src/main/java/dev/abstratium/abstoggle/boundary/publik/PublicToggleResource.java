package dev.abstratium.abstoggle.boundary.publik;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.abstratium.abstoggle.dto.QueryResponse;
import dev.abstratium.abstoggle.service.ToggleQueryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
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
}
