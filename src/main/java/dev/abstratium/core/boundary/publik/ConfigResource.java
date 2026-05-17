package dev.abstratium.core.boundary.publik;

import dev.abstratium.core.BuildInfo;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/public/config")
@Tag(name = "API", description = "Public API endpoints")
public class ConfigResource {

    @ConfigProperty(name = "client.log.level")
    String clientLogLevel;

    @ConfigProperty(name = "warning.message", defaultValue = "-")
    String warningMessage;

    @ConfigProperty(name = "abstratium.stage", defaultValue = "dev")
    String stage;

    @ConfigProperty(name = "toggle.change-note.mandatory", defaultValue = "true")
    boolean changeNoteMandatory;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SuccessResponse config() {
        return new SuccessResponse(clientLogLevel, BuildInfo.BUILD_TIMESTAMP, warningMessage, stage, changeNoteMandatory);
    }

    @RegisterForReflection
    public static class SuccessResponse {
        public String logLevel;
        public String baselineBuildTimestamp;
        public String warningMessage;
        public String stage;
        public boolean changeNoteMandatory;

        public SuccessResponse(String logLevel, String baselineBuildTimestamp, String warningMessage, String stage, boolean changeNoteMandatory) {
            this.logLevel = logLevel;
            this.baselineBuildTimestamp = baselineBuildTimestamp;
            this.warningMessage = warningMessage;
            this.stage = stage;
            this.changeNoteMandatory = changeNoteMandatory;
        }
    }
}
