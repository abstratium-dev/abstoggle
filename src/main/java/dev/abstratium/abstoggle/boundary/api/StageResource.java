package dev.abstratium.abstoggle.boundary.api;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.abstratium.abstoggle.Roles;
import dev.abstratium.abstoggle.dto.StageDto;
import dev.abstratium.abstoggle.entity.Stage;
import dev.abstratium.abstoggle.service.StageService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/stages")
@Tag(name = "Stage Management", description = "Stage management endpoints")
public class StageResource {

    @Inject
    StageService stageService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public List<StageDto> getAllStages() {
        List<Stage> stages = stageService.findAll();
        return stages.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public StageDto createStage(StageDto request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Stage name is required");
        }

        Stage stage = stageService.create(
            request.getName(),
            request.getDescription(),
            request.getDisplayOrder(),
            request.getParentStageName()
        );

        return convertToDto(stage);
    }

    @PUT
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public StageDto updateStage(@PathParam("id") String id, StageDto request) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Stage ID is required");
        }
        
        Stage existingStage = stageService.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + id));
        
        Stage stage = stageService.update(
            existingStage.getId(),
            request.getName(),
            request.getDescription(),
            request.getDisplayOrder(),
            request.getParentStageName()
        );
        
        return convertToDto(stage);
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public Response deleteStage(@PathParam("id") String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Stage ID is required");
        }
        
        stageService.delete(id);
        return Response.ok().build();
    }

    private StageDto convertToDto(Stage stage) {
        StageDto dto = new StageDto();
        dto.setId(stage.getId());
        dto.setName(stage.getName());
        dto.setDescription(stage.getDescription());
        dto.setDisplayOrder(stage.getDisplayOrder());

        if (stage.getParentStage() != null) {
            dto.setParentStageName(stage.getParentStage().getName());
        }

        return dto;
    }
}
