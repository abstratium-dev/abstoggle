package dev.abstratium.abstoggle.dto;

import java.util.Map;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ToggleStageRuleDto {
    private String id;
    private String toggleName;
    private String stageName;
    private String ruleId;
    private String ruleName;
    private String ruleValue;
    private String description;
    private Integer priority;
    private Map<String, String> criteria;

    public ToggleStageRuleDto() {}

    public ToggleStageRuleDto(String id, String toggleName, String stageName, String ruleId,
                                 String ruleName, String ruleValue, String description,
                                 Integer priority, Map<String, String> criteria) {
        this.id = id;
        this.toggleName = toggleName;
        this.stageName = stageName;
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.ruleValue = ruleValue;
        this.description = description;
        this.priority = priority;
        this.criteria = criteria;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getToggleName() {
        return toggleName;
    }

    public void setToggleName(String toggleName) {
        this.toggleName = toggleName;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRuleValue() {
        return ruleValue;
    }

    public void setRuleValue(String ruleValue) {
        this.ruleValue = ruleValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Map<String, String> getCriteria() {
        return criteria;
    }

    public void setCriteria(Map<String, String> criteria) {
        this.criteria = criteria;
    }
}
