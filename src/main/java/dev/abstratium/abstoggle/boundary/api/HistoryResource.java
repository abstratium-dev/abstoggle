package dev.abstratium.abstoggle.boundary.api;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.abstratium.abstoggle.Roles;
import dev.abstratium.abstoggle.dto.EntityRevisionDto;
import dev.abstratium.abstoggle.dto.HistoryChangeDto;
import dev.abstratium.abstoggle.dto.HistoryEntryDto;
import dev.abstratium.abstoggle.service.HistoryService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/api/history")
@Tag(name = "History", description = "Audit history endpoints")
public class HistoryResource {

    @Inject
    HistoryService historyService;

    /**
     * Search revision history from REVINFO.
     * Optionally filter by a search term matched against username or change note.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public List<HistoryEntryDto> searchHistory(
            @QueryParam("search") String search,
            @QueryParam("limit") @DefaultValue("50") int limit,
            @QueryParam("offset") @DefaultValue("0") int offset) {

        if (limit < 1 || limit > 200) {
            throw new IllegalArgumentException("limit must be between 1 and 200");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0");
        }

        return historyService.searchHistory(search, limit, offset);
    }

    /**
     * Get detailed entity changes for a specific revision number.
     */
    @GET
    @Path("/{rev}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public List<HistoryChangeDto> getRevisionDetails(@PathParam("rev") long rev) {
        if (rev < 1) {
            throw new IllegalArgumentException("rev must be >= 1");
        }
        return historyService.getRevisionDetails(rev);
    }

    /**
     * Get the full audit history for a specific entity by table name and ID.
     */
    @GET
    @Path("/entity/{table}/{entityId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public List<EntityRevisionDto> getEntityHistory(
            @PathParam("table") String table,
            @PathParam("entityId") String entityId) {
        if (table == null || table.isBlank()) {
            throw new IllegalArgumentException("table is required");
        }
        if (entityId == null || entityId.isBlank()) {
            throw new IllegalArgumentException("entityId is required");
        }
        return historyService.getEntityHistory(table, entityId);
    }
}
