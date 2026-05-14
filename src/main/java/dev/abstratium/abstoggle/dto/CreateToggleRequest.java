package dev.abstratium.abstoggle.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CreateToggleRequest {
    private String name;
    private String description;
    private Boolean enabled;

    public CreateToggleRequest() {}

    public CreateToggleRequest(String name, String description, Boolean enabled) {
        this.name = name;
        this.description = description;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
