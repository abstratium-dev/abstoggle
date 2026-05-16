package dev.abstratium.abstoggle.dto;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class RuleDto {
    private String id;
    private String name;
    private String description;
    private List<CriterionDto> criteria;

    public RuleDto() {}

    public RuleDto(String id, String name, String description, List<CriterionDto> criteria) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.criteria = criteria;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public List<CriterionDto> getCriteria() {
        return criteria;
    }

    public void setCriteria(List<CriterionDto> criteria) {
        this.criteria = criteria;
    }
}
