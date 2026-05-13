package dev.abstratium.abstoggle.boundary.api;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.abstratium.abstoggle.dto.ToggleQueryResponse;
import dev.abstratium.abstoggle.service.ToggleQueryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/public/toggles")
@Tag(name = "Public Toggles", description = "Public toggle endpoints (no authentication required)")
public class PublicToggleResource {

    @Inject
    ToggleQueryService toggleQueryService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ToggleQueryResponse queryToggles(
            @QueryParam("stage") String stage,
            @QueryParam("nameFilter") String nameFilter,
            @QueryParam("includeDisabled") Boolean includeDisabled) {
        
        // Validate required parameter
        if (stage == null || stage.trim().isEmpty()) {
            throw new IllegalArgumentException("Stage parameter is required");
        }
        
        // Default includeDisabled to false for public endpoint
        if (includeDisabled == null) {
            includeDisabled = false;
        }
        
        return toggleQueryService.queryToggles(stage, nameFilter, includeDisabled);
    }
}
