package dev.abstratium.abstoggle.boundary.api;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.abstratium.abstoggle.Roles;
import dev.abstratium.abstoggle.dto.QueryResponse;
import dev.abstratium.abstoggle.dto.ToggleDto;
import dev.abstratium.abstoggle.entity.Toggle;
import dev.abstratium.abstoggle.service.ToggleQueryService;
import dev.abstratium.abstoggle.service.ToggleService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/toggles")
@Tag(name = "Toggle Management", description = "Toggle CRUD endpoints")
public class ToggleResource {

    @Inject
    ToggleService toggleService;

    @Inject
    ToggleQueryService toggleQueryService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public QueryResponse queryToggles(
            @QueryParam("stage") String stage,
            @QueryParam("context") String context,
            @QueryParam("nameFilter") String nameFilter,
            @QueryParam("includeDisabled") Boolean includeDisabled) {

        // Validate required parameter
        if (stage == null || stage.trim().isEmpty()) {
            throw new IllegalArgumentException("Stage parameter is required");
        }

        // Use non-cached query for management endpoints to ensure fresh data
        return toggleQueryService.queryTogglesWithoutCache(stage, context, nameFilter, includeDisabled);
    }

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public List<ToggleDto> getAllToggles(
            @QueryParam("assignedToStage") String assignedToStage,
            @QueryParam("assignedToRule") String assignedToRule) {
        List<Toggle> toggles = toggleService.findAll(assignedToStage, assignedToRule);
        return toggles.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    @GET
    @Path("/contexts")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public List<String> getDistinctContexts() {
        return toggleService.getDistinctContexts();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public ToggleDto createToggle(ToggleDto request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Toggle name is required");
        }

        Toggle toggle = toggleService.create(
            request.getName(),
            request.getDescription(),
            request.getEnabled(),
            request.getContext()
        );
        
        return convertToDto(toggle);
    }

    @PUT
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public ToggleDto updateToggle(@PathParam("id") String id, ToggleDto request) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Toggle id is required");
        }
        
        Toggle toggle = toggleService.update(id,
            request.getName(),
            request.getDescription(),
            request.getEnabled(),
            request.getContext()
        );
        
        return convertToDto(toggle);
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public Response deleteToggle(@PathParam("id") String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Toggle id is required");
        }
        
        toggleService.delete(id);
        return Response.ok().build();
    }

    private ToggleDto convertToDto(Toggle toggle) {
        return new ToggleDto(toggle.getId(), toggle.getName(), toggle.getDescription(), toggle.getEnabled(), toggle.getContext());
    }
}
