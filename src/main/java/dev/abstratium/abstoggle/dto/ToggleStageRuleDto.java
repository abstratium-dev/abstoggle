package dev.abstratium.abstoggle.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * DTO for Toggle-Stage-Rule assignments using UUID IDs.
 */
@RegisterForReflection
public class ToggleStageRuleDto {
    private String id;
    private String toggleId;
    private String stageId;
    private String ruleId;
    private String ruleValue;
    private Integer priority;

    public ToggleStageRuleDto() {}

    public ToggleStageRuleDto(String id, String toggleId, String stageId, String ruleId, String ruleValue, Integer priority) {
        this.id = id;
        this.toggleId = toggleId;
        this.stageId = stageId;
        this.ruleId = ruleId;
        this.ruleValue = ruleValue;
        this.priority = priority;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getToggleId() {
        return toggleId;
    }

    public void setToggleId(String toggleId) {
        this.toggleId = toggleId;
    }

    public String getStageId() {
        return stageId;
    }

    public void setStageId(String stageId) {
        this.stageId = stageId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
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
}
