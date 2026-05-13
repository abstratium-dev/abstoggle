package dev.abstratium.abstoggle.boundary.api;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.abstratium.abstoggle.Roles;
import dev.abstratium.abstoggle.dto.CreateStageRequest;
import dev.abstratium.abstoggle.dto.UpdateStageRequest;
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

@Path("/api/admin/stages")
@Tag(name = "Stage Administration", description = "Stage management endpoints")
public class AdminStageResource {

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
    public StageDto createStage(CreateStageRequest request) {
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
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public StageDto updateStage(@PathParam("name") String name, UpdateStageRequest request) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Stage name is required");
        }
        
        // Find existing stage to get its ID
        Stage existingStage = stageService.findByName(name)
            .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + name));
        
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
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public Response deleteStage(@PathParam("name") String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Stage name is required");
        }
        
        stageService.deleteByName(name);
        return Response.ok().build();
    }

    private StageDto convertToDto(Stage stage) {
        StageDto dto = new StageDto();
        dto.setId(stage.getId());
        dto.setName(stage.getName());
        dto.setDescription(stage.getDescription());
        dto.setDisplayOrder(stage.getDisplayOrder());
        dto.setCreatedAt(stage.getCreatedAt());
        
        // Set parent stage info if exists
        if (stage.getParentStage() != null) {
            dto.setParentStageName(stage.getParentStage().getName());
        }
        
        return dto;
    }

    // DTO for stage responses
    public static class StageDto {
        private String id;
        private String name;
        private String description;
        private Integer displayOrder;
        private String parentStageName;
        private java.time.Instant createdAt;

        public StageDto() {}

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getDisplayOrder() {
            return displayOrder;
        }

        public void setDisplayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
        }

        public String getParentStageName() {
            return parentStageName;
        }

        public void setParentStageName(String parentStageName) {
            this.parentStageName = parentStageName;
        }

        public java.time.Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(java.time.Instant createdAt) {
            this.createdAt = createdAt;
        }
    }
}
