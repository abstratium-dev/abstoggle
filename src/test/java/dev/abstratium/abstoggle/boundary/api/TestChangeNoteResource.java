package dev.abstratium.abstoggle.boundary.api;

import dev.abstratium.abstoggle.boundary.interceptor.RequiresChangeNote;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Test resource endpoint for verifying @RequiresChangeNote interceptor behavior.
 * Only available in test mode.
 */
@Path("/api/test-change-note")
@RegisterForReflection
public class TestChangeNoteResource {

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @RequiresChangeNote
    public Response testEndpoint() {
        return Response.ok("success").build();
    }
}
