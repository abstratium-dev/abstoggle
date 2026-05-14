package dev.abstratium.abstoggle.boundary.api;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.abstratium.abstoggle.Roles;
import dev.abstratium.abstoggle.dto.CreateRuleRequest;
import dev.abstratium.abstoggle.dto.RuleDto;
import dev.abstratium.abstoggle.dto.UpdateRuleRequest;
import dev.abstratium.abstoggle.entity.ToggleRule;
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

@Path("/api/toggles/{name}/stages/{stageName}/rules")
@Tag(name = "Toggle Rule Management", description = "Toggle rule endpoints")
public class ToggleRuleResource {

    @Inject
    ToggleRuleService toggleRuleService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public List<RuleDto> getRulesForToggle(@PathParam("name") String toggleName, @PathParam("stageName") String stageName) {
        if (toggleName == null || toggleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Toggle name is required");
        }
        if (stageName == null || stageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Stage name is required");
        }
        
        List<ToggleRule> rules = toggleRuleService.findByToggleAndStage(toggleName, stageName);
        return rules.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public RuleDto createRule(@PathParam("name") String toggleName, @PathParam("stageName") String stageName, CreateRuleRequest request) {
        if (toggleName == null || toggleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Toggle name is required");
        }
        if (stageName == null || stageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Stage name is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        
        ToggleRule rule = toggleRuleService.createRule(
            toggleName,
            stageName,
            request.getRuleValue(),
            request.getPriority(),
            request.getDescription(),
            request.getCriteriaData()
        );
        
        return convertToDto(rule);
    }

    @PUT
    @Path("/{ruleId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public RuleDto updateRule(@PathParam("name") String toggleName, @PathParam("stageName") String stageName, 
                             @PathParam("ruleId") String ruleId, UpdateRuleRequest request) {
        if (toggleName == null || toggleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Toggle name is required");
        }
        if (stageName == null || stageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Stage name is required");
        }
        if (ruleId == null || ruleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule ID is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        
        ToggleRule rule = toggleRuleService.updateRule(
            ruleId,
            request.getRuleValue(),
            request.getPriority(),
            request.getDescription(),
            request.getCriteriaData()
        );
        
        return convertToDto(rule);
    }

    @DELETE
    @Path("/{ruleId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public Response deleteRule(@PathParam("name") String toggleName, @PathParam("stageName") String stageName, 
                              @PathParam("ruleId") String ruleId) {
        if (toggleName == null || toggleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Toggle name is required");
        }
        if (stageName == null || stageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Stage name is required");
        }
        if (ruleId == null || ruleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule ID is required");
        }
        
        toggleRuleService.deleteRule(ruleId);
        return Response.ok().build();
    }

    private RuleDto convertToDto(ToggleRule rule) {
        // Get criteria for this rule
        List<dev.abstratium.abstoggle.entity.ToggleCriterion> criteria = toggleRuleService.getCriteriaForRule(rule.getId());
        
        // Build criteria map
        java.util.Map<String, String> criteriaMap = new java.util.HashMap<>();
        for (dev.abstratium.abstoggle.entity.ToggleCriterion criterion : criteria) {
            criteriaMap.put(criterion.getCriterionKey(), criterion.getCriterionValue());
        }
        
        return new RuleDto(
            rule.getId(),
            rule.getPriority(),
            rule.getRuleValue(),
            rule.getDescription(),
            criteriaMap
        );
    }
}
