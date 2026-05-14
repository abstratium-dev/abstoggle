package dev.abstratium.abstoggle.dto;

import java.util.Map;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class RuleDto {
    private String id;
    private Integer priority;
    private String value;
    private String description;
    private Map<String, String> criteria;

    public RuleDto() {}

    public RuleDto(String id, Integer priority, String value, String description, Map<String, String> criteria) {
        this.id = id;
        this.priority = priority;
        this.value = value;
        this.description = description;
        this.criteria = criteria;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getCriteria() {
        return criteria;
    }

    public void setCriteria(Map<String, String> criteria) {
        this.criteria = criteria;
    }
}
