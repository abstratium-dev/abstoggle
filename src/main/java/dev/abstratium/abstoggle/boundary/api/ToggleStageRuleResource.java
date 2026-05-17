package dev.abstratium.abstoggle.boundary.api;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.abstratium.abstoggle.Roles;
import dev.abstratium.abstoggle.boundary.interceptor.RequiresChangeNote;
import dev.abstratium.abstoggle.dto.ToggleStageRuleDto;
import dev.abstratium.abstoggle.entity.Stage;
import dev.abstratium.abstoggle.entity.Toggle;
import dev.abstratium.abstoggle.entity.ToggleStageRule;
import dev.abstratium.abstoggle.service.StageService;
import dev.abstratium.abstoggle.service.ToggleService;
import dev.abstratium.abstoggle.service.ToggleStageRuleService;
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

@Path("/api/toggle-stage-rules")
@Tag(name = "Toggle-Stage-Rule Assignment Management", description = "Manage toggle-stage-rule assignments")
public class ToggleStageRuleResource {

    @Inject
    ToggleStageRuleService toggleStageRuleService;

    @Inject
    ToggleService toggleService;

    @Inject
    StageService stageService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public List<ToggleStageRuleDto> getAllAssignments(
            @QueryParam("toggleId") String toggleId,
            @QueryParam("stageId") String stageId) {
        List<ToggleStageRule> assignments;
        
        if (toggleId != null && !toggleId.trim().isEmpty()) {
            Optional<Toggle> toggle = toggleService.findById(toggleId);
            assignments = toggle.map(t -> toggleStageRuleService.findByToggleName(t.getName()))
                .orElse(List.of());
        } else if (stageId != null && !stageId.trim().isEmpty()) {
            Optional<Stage> stage = stageService.findById(stageId);
            assignments = stage.map(s -> toggleStageRuleService.findByStageName(s.getName()))
                .orElse(List.of());
        } else {
            assignments = toggleStageRuleService.findAll();
        }
        
        return assignments.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    @RequiresChangeNote
    public ToggleStageRuleDto createAssignment(ToggleStageRuleDto request) {
        if (request.getToggleId() == null || request.getToggleId().trim().isEmpty()) {
            throw new IllegalArgumentException("Toggle ID is required");
        }
        if (request.getStageId() == null || request.getStageId().trim().isEmpty()) {
            throw new IllegalArgumentException("Stage ID is required");
        }
        if (request.getRuleId() == null || request.getRuleId().trim().isEmpty()) {
            throw new IllegalArgumentException("Rule ID is required");
        }

        ToggleStageRule tsr = toggleStageRuleService.create(
            request.getToggleId(),
            request.getStageId(),
            request.getRuleId(),
            request.getToggleValue(),
            request.getPriority()
        );

        return convertToDto(tsr);
    }

    @PUT
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    @RequiresChangeNote
    public ToggleStageRuleDto updateAssignment(@PathParam("id") String id, ToggleStageRuleDto request) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Assignment ID is required");
        }

        ToggleStageRule tsr = toggleStageRuleService.update(
            id,
            request.getToggleValue(),
            request.getPriority()
        );

        return convertToDto(tsr);
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    @RequiresChangeNote
    public Response deleteAssignment(@PathParam("id") String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Assignment ID is required");
        }

        toggleStageRuleService.delete(id);
        return Response.ok().build();
    }

    private ToggleStageRuleDto convertToDto(ToggleStageRule tsr) {
        return new ToggleStageRuleDto(
            tsr.getId(),
            tsr.getToggle().getId(),
            tsr.getStage().getId(),
            tsr.getRule().getId(),
            tsr.getToggleValue(),
            tsr.getPriority()
        );
    }
}
