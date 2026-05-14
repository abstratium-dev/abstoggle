package dev.abstratium.abstoggle.entity;

import java.util.UUID;

import org.hibernate.envers.Audited;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "T_toggle_rule")
@Audited
public class ToggleRule {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "toggle_stage_id",
        nullable = false,
        foreignKey = @jakarta.persistence.ForeignKey(name = "FK_toggle_rule_stage_id")
    )
    private ToggleStage toggleStage;

    @Column(name = "rule_value", length = 255, nullable = false)
    private String ruleValue = "off";

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer priority = 100;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ToggleStage getToggleStage() {
        return toggleStage;
    }

    public void setToggleStage(ToggleStage toggleStage) {
        this.toggleStage = toggleStage;
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
}
