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
    private String toggleValue;
    private Integer priority;

    public ToggleStageRuleDto() {}

    public ToggleStageRuleDto(String id, String toggleId, String stageId, String ruleId, String toggleValue, Integer priority) {
        this.id = id;
        this.toggleId = toggleId;
        this.stageId = stageId;
        this.ruleId = ruleId;
        this.toggleValue = toggleValue;
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

    public String getToggleValue() {
        return toggleValue;
    }

    public void setToggleValue(String toggleValue) {
        this.toggleValue = toggleValue;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
