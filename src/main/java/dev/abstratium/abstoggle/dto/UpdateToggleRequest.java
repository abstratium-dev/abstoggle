package dev.abstratium.abstoggle.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class UpdateToggleRequest {
    private String description;
    private Boolean enabled;
    private String context;

    public UpdateToggleRequest() {}

    public UpdateToggleRequest(String description, Boolean enabled, String context) {
        this.description = description;
        this.enabled = enabled;
        this.context = context;
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

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
