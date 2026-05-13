package dev.abstratium.abstoggle.dto;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ToggleDto {
    private String name;
    private String stage;
    private String description;
    private List<RuleDto> rules;

    public ToggleDto() {}

    public ToggleDto(String name, String stage, String description, List<RuleDto> rules) {
        this.name = name;
        this.stage = stage;
        this.description = description;
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
}
