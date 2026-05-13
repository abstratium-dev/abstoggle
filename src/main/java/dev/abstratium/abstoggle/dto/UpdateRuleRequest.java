package dev.abstratium.abstoggle.dto;

import java.util.Map;

import dev.abstratium.abstoggle.service.ToggleRuleService.CriterionData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class UpdateRuleRequest {
    private String ruleValue;
    private Integer priority;
    private String description;
    private Map<String, String> criteria;

    public UpdateRuleRequest() {}

    public UpdateRuleRequest(String ruleValue, Integer priority, String description, Map<String, String> criteria) {
        this.ruleValue = ruleValue;
        this.priority = priority;
        this.description = description;
        this.criteria = criteria;
    }

    public String getRuleValue() {
        return ruleValue;
    }

    public void setRuleValue(String ruleValue) {
        this.ruleValue = ruleValue;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
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

    public java.util.List<CriterionData> getCriteriaData() {
        if (criteria == null) {
            return java.util.List.of();
        }
        return criteria.entrySet().stream()
            .map(entry -> new CriterionData(entry.getKey(), entry.getValue()))
            .toList();
    }
}
