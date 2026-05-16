package dev.abstratium.abstoggle.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CriterionDto {
    private String id;
    private String criterionKey;
    private String criterionValue;
    private String ruleId;

    public CriterionDto() {}

    public CriterionDto(String id, String criterionKey, String criterionValue, String ruleId) {
        this.id = id;
        this.criterionKey = criterionKey;
        this.criterionValue = criterionValue;
        this.ruleId = ruleId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCriterionKey() {
        return criterionKey;
    }

    public void setCriterionKey(String criterionKey) {
        this.criterionKey = criterionKey;
    }

    public String getCriterionValue() {
        return criterionValue;
    }

    public void setCriterionValue(String criterionValue) {
        this.criterionValue = criterionValue;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }
}
