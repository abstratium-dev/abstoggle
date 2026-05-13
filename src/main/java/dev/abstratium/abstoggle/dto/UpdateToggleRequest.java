package dev.abstratium.abstoggle.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class UpdateToggleRequest {
    private String description;
    private Boolean enabled;

    public UpdateToggleRequest() {}

    public UpdateToggleRequest(String description, Boolean enabled) {
        this.description = description;
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
