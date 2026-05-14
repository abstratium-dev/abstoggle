package dev.abstratium.abstoggle.boundary.api;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.abstratium.abstoggle.Roles;
import dev.abstratium.abstoggle.dto.CreateToggleRequest;
import dev.abstratium.abstoggle.dto.ToggleDto;
import dev.abstratium.abstoggle.dto.ToggleQueryResponse;
import dev.abstratium.abstoggle.dto.UpdateToggleRequest;
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
    public ToggleQueryResponse queryToggles(
            @QueryParam("stage") String stage,
            @QueryParam("nameFilter") String nameFilter,
            @QueryParam("includeDisabled") Boolean includeDisabled) {

        // Validate required parameter
        if (stage == null || stage.trim().isEmpty()) {
            throw new IllegalArgumentException("Stage parameter is required");
        }

        // Use non-cached query for management endpoints to ensure fresh data
        return toggleQueryService.queryTogglesWithoutCache(stage, null, nameFilter, includeDisabled);
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

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public ToggleDto createToggle(CreateToggleRequest request) {
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
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public ToggleDto updateToggle(@PathParam("name") String name, UpdateToggleRequest request) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Toggle name is required");
        }
        
        Toggle toggle = toggleService.updateByName(
            name,
            request.getDescription(),
            request.getEnabled(),
            request.getContext()
        );
        
        return convertToDto(toggle);
    }

    @DELETE
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public Response deleteToggle(@PathParam("name") String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Toggle name is required");
        }
        
        toggleService.deleteByName(name);
        return Response.ok().build();
    }

    private ToggleDto convertToDto(Toggle toggle) {
        // Simple DTO without rules for basic toggle operations
        return new ToggleDto(toggle.getName(), null, toggle.getDescription(), toggle.getEnabled(), toggle.getContext(), null);
    }
}
