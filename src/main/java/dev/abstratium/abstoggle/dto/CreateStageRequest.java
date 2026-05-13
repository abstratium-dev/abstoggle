package dev.abstratium.abstoggle.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CreateStageRequest {
    private String name;
    private String description;
    private Integer displayOrder;
    private String parentStageName;

    public CreateStageRequest() {}

    public CreateStageRequest(String name, String description, Integer displayOrder, String parentStageName) {
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
        this.parentStageName = parentStageName;
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

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getParentStageName() {
        return parentStageName;
    }

    public void setParentStageName(String parentStageName) {
        this.parentStageName = parentStageName;
    }
}
