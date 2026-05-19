package dev.abstratium.abstoggle.boundary.api;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.abstratium.abstoggle.Roles;
import dev.abstratium.abstoggle.boundary.interceptor.RequiresChangeNote;
import dev.abstratium.abstoggle.dto.CriterionDto;
import dev.abstratium.abstoggle.dto.RuleDto;
import dev.abstratium.abstoggle.entity.Criterion;
import dev.abstratium.abstoggle.entity.Rule;
import dev.abstratium.abstoggle.service.RuleService;
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
    RuleService ruleService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public List<RuleDto> getAllRules() {
        List<Rule> rules = ruleService.findAll();
        return rules.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    @RequiresChangeNote
    public RuleDto createRule(RuleDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        Rule rule = ruleService.createRule(
            request.getName(),
            request.getDescription(),
            request.getCriteria()
        );
        return convertToDto(rule);
    }

    @PUT
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    @RequiresChangeNote
    public RuleDto updateRule(@PathParam("id") String id, RuleDto request) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule ID is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        Rule rule = ruleService.updateRule(
            id,
            request.getName(),
            request.getDescription(),
            request.getCriteria()
        );
        return convertToDto(rule);
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    @RequiresChangeNote
    public Response deleteRule(@PathParam("id") String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule ID is required");
        }
        ruleService.deleteRule(id);
        return Response.ok().build();
    }

    private RuleDto convertToDto(Rule rule) {
        List<Criterion> criteria = ruleService.getCriteriaForRule(rule.getId());
        List<CriterionDto> criteriaMap = criteria.stream()
            .map(criterion -> new CriterionDto(criterion.getId(), criterion.getCriterionKey(), criterion.getCriterionValue(), criterion.getId()))
            .collect(Collectors.toList());

        return new RuleDto(
            rule.getId(),
            rule.getName(),
            rule.getDescription(),
            criteriaMap
        );
    }
}
