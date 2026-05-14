package dev.abstratium.abstoggle.dto;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ToggleDto {
    private String name;
    private String stage;
    private String description;
    private Boolean enabled;
    private String context;
    private List<RuleDto> rules;

    public ToggleDto() {}

    public ToggleDto(String name, String stage, String description, Boolean enabled, String context, List<RuleDto> rules) {
        this.name = name;
        this.stage = stage;
        this.description = description;
        this.enabled = enabled;
        this.context = context;
        this.rules = rules;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<RuleDto> getRules() {
        return rules;
    }

    public void setRules(List<RuleDto> rules) {
        this.rules = rules;
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
