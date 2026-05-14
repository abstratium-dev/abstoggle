package dev.abstratium.abstoggle.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CreateToggleStageRuleRequest {
    private String stageName;
    private String ruleId;
    private Integer priority;

    public CreateToggleStageRuleRequest() {}

    public CreateToggleStageRuleRequest(String stageName, String ruleId, Integer priority) {
        this.stageName = stageName;
        this.ruleId = ruleId;
        this.priority = priority;
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

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
