package dev.abstratium.abstoggle.boundary.api;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.abstratium.abstoggle.Roles;
import dev.abstratium.abstoggle.dto.CreateToggleStageRuleRequest;
import dev.abstratium.abstoggle.dto.ToggleStageRuleDto;
import dev.abstratium.abstoggle.entity.ToggleCriterion;
import dev.abstratium.abstoggle.entity.ToggleStageRule;
import dev.abstratium.abstoggle.service.ToggleRuleService;
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

@Path("/api/toggles/{name}/stage-rules")
@Tag(name = "Toggle Stage Rule Management", description = "Toggle stage rule assignment endpoints")
public class ToggleStageRuleResource {

    @Inject
    ToggleRuleService toggleRuleService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public List<ToggleStageRuleDto> getStageRulesForToggle(@PathParam("name") String toggleName) {
        if (toggleName == null || toggleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Toggle name is required");
        }

        List<ToggleStageRule> assignments = toggleRuleService.getAssignmentsForToggle(toggleName);
        return assignments.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public Response createStageRule(@PathParam("name") String toggleName,
                                     CreateToggleStageRuleRequest request) {
        if (toggleName == null || toggleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Toggle name is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getStageName() == null || request.getStageName().trim().isEmpty()) {
            throw new IllegalArgumentException("Stage name is required");
        }
        if (request.getRuleId() == null || request.getRuleId().trim().isEmpty()) {
            throw new IllegalArgumentException("Rule ID is required");
        }

        toggleRuleService.assignRule(
            toggleName,
            request.getStageName(),
            request.getRuleId(),
            request.getPriority()
        );
        return Response.ok().build();
    }

    @PUT
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public ToggleStageRuleDto updateStageRulePriority(@PathParam("name") String toggleName,
                                                       @PathParam("id") String id,
                                                       CreateToggleStageRuleRequest request) {
        if (toggleName == null || toggleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Toggle name is required");
        }
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Assignment ID is required");
        }

        ToggleStageRule assignment = toggleRuleService.updateAssignmentPriorityById(
            id,
            request != null ? request.getPriority() : null
        );
        return convertToDto(assignment);
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public Response deleteStageRule(@PathParam("name") String toggleName,
                                     @PathParam("id") String id) {
        if (toggleName == null || toggleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Toggle name is required");
        }
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Assignment ID is required");
        }

        toggleRuleService.unassignById(id);
        return Response.ok().build();
    }

    private ToggleStageRuleDto convertToDto(ToggleStageRule assignment) {
        List<ToggleCriterion> criteria = toggleRuleService.getCriteriaForRule(assignment.getRule().getId());
        Map<String, String> criteriaMap = new java.util.HashMap<>();
        for (ToggleCriterion criterion : criteria) {
            criteriaMap.put(criterion.getCriterionKey(), criterion.getCriterionValue());
        }

        return new ToggleStageRuleDto(
            assignment.getId(),
            assignment.getToggle().getName(),
            assignment.getStage().getName(),
            assignment.getRule().getId(),
            assignment.getRule().getName(),
            assignment.getRule().getRuleValue(),
            assignment.getRule().getDescription(),
            assignment.getPriority(),
            criteriaMap
        );
    }
}
