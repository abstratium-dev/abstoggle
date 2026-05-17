package dev.abstratium.abstoggle.boundary.interceptor;

import io.quarkiverse.resteasy.problem.HttpProblem;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Interceptor for {@link RequiresChangeNote} annotation.
 * Validates that the "changeNote" query parameter is present and non-empty before method execution
 * when the toggle.change-note.mandatory configuration is true.
 * Stores the change note in the request-scoped {@link ChangeNoteContext} for later use.
 */
@Interceptor
@RequiresChangeNote
@Priority(Interceptor.Priority.APPLICATION)
public class ChangeNoteInterceptor {

    private static final String CHANGE_NOTE_PARAM = "changeNote";

    @Context
    UriInfo uriInfo;

    @Inject
    ChangeNoteContext changeNoteContext;

    @ConfigProperty(name = "toggle.change-note.mandatory", defaultValue = "true")
    boolean changeNoteMandatory;

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        String changeNote = uriInfo.getQueryParameters().getFirst(CHANGE_NOTE_PARAM);

        // If change notes are mandatory and none provided, reject the request
        if (changeNoteMandatory && (changeNote == null || changeNote.isBlank())) {
            throw HttpProblem.builder()
                    .withStatus(Response.Status.BAD_REQUEST)
                    .withTitle("Bad Request")
                    .withDetail("Missing required query parameter: " + CHANGE_NOTE_PARAM)
                    .build();
        }

        // Store the change note if provided (even when optional)
        if (changeNote != null && !changeNote.isBlank()) {
            changeNoteContext.setChangeNote(changeNote);
        }

        return ctx.proceed();
    }
}
