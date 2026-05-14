package dev.abstratium.abstoggle.boundary.api;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.abstratium.abstoggle.Roles;
import dev.abstratium.abstoggle.dto.RuleDto;
import dev.abstratium.abstoggle.entity.ToggleCriterion;
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

@Path("/api/rules")
@Tag(name = "Rule Management", description = "Reusable rule management endpoints")
public class RuleResource {

    @Inject
    ToggleRuleService toggleRuleService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public List<RuleDto> getAllRules() {
        List<ToggleRule> rules = toggleRuleService.findAll();
        return rules.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public RuleDto createRule(dev.abstratium.abstoggle.dto.CreateRuleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        ToggleRule rule = toggleRuleService.createStandaloneRule(
            request.getName(),
            request.getRuleValue(),
            request.getDescription(),
            request.getCriteriaData()
        );
        return convertToDto(rule);
    }

    @PUT
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public RuleDto updateRule(@PathParam("id") String id, dev.abstratium.abstoggle.dto.UpdateRuleRequest request) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule ID is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        ToggleRule rule = toggleRuleService.updateStandaloneRule(
            id,
            request.getName(),
            request.getRuleValue(),
            request.getDescription(),
            request.getCriteriaData()
        );
        return convertToDto(rule);
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public Response deleteRule(@PathParam("id") String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule ID is required");
        }
        toggleRuleService.deleteRule(id);
        return Response.ok().build();
    }

    private RuleDto convertToDto(ToggleRule rule) {
        List<ToggleCriterion> criteria = toggleRuleService.getCriteriaForRule(rule.getId());
        Map<String, String> criteriaMap = new java.util.HashMap<>();
        for (ToggleCriterion criterion : criteria) {
            criteriaMap.put(criterion.getCriterionKey(), criterion.getCriterionValue());
        }

        return new RuleDto(
            rule.getId(),
            rule.getName(),
            null, // priority is assignment-specific
            rule.getRuleValue(),
            rule.getDescription(),
            criteriaMap
        );
    }
}
