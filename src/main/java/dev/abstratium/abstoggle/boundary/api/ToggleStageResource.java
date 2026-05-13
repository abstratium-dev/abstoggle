package dev.abstratium.abstoggle.boundary.api;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.abstratium.abstoggle.Roles;
import dev.abstratium.abstoggle.entity.ToggleStage;
import dev.abstratium.abstoggle.service.ToggleStageService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/toggles/{name}/stages")
@Tag(name = "Toggle Stage Management", description = "Toggle-stage relationship endpoints")
public class ToggleStageResource {

    @Inject
    ToggleStageService toggleStageService;

    @POST
    @Path("/{stageName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public Response addStageToToggle(@PathParam("name") String toggleName, @PathParam("stageName") String stageName) {
        if (toggleName == null || toggleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Toggle name is required");
        }
        if (stageName == null || stageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Stage name is required");
        }
        
        ToggleStage toggleStage = toggleStageService.addStageToToggle(toggleName, stageName);
        return Response.ok(toggleStage).build();
    }

    @DELETE
    @Path("/{stageName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Roles.USER})
    public Response removeStageFromToggle(@PathParam("name") String toggleName, @PathParam("stageName") String stageName) {
        if (toggleName == null || toggleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Toggle name is required");
        }
        if (stageName == null || stageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Stage name is required");
        }
        
        toggleStageService.removeStageFromToggle(toggleName, stageName);
        return Response.ok().build();
    }
}
