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
import jakarta.persistence.UniqueConstraint;

/**
 * Represents an assignment of a {@link Rule} to a {@link Toggle} within
 * a specific {@link Stage}, along with a value and an evaluation priority.
 */
@Entity
@Table(
    name = "T_toggle_stage_rule",
    uniqueConstraints = @UniqueConstraint(
        name = "UQ_toggle_stage_rule_toggle_stage_rule",
        columnNames = {"toggle_id", "stage_id", "rule_id"}
    )
)
@Audited
public class ToggleStageRule {

    /**
     * v4 UUID generated in Java code.
     */
    @Id
    @Column(length = 36)
    private String id;

    /**
     * Reference to the {@link Toggle} this rule is assigned to.
     * Deleting the toggle cascades and removes this assignment.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "toggle_id",
        nullable = false,
        foreignKey = @jakarta.persistence.ForeignKey(name = "FK_toggle_stage_rule_toggle_id")
    )
    private Toggle toggle;

    /**
     * Reference to the {@link Stage} this rule is assigned within.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "stage_id",
        nullable = false,
        foreignKey = @jakarta.persistence.ForeignKey(name = "FK_toggle_stage_rule_stage_id")
    )
    private Stage stage;

    /**
     * Reference to the reusable {@link Rule} being assigned.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "rule_id",
        nullable = false,
        foreignKey = @jakarta.persistence.ForeignKey(name = "FK_toggle_stage_rule_rule_id")
    )
    private Rule rule;

    /**
     * Value returned when this rule's criteria match (default: "off").
     */
    @Column(name = "rule_value", length = 255, nullable = false)
    private String ruleValue = "off";

    /**
     * Evaluation order within this toggle and stage. Lower values are evaluated first.
     */
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

    public Toggle getToggle() {
        return toggle;
    }

    public void setToggle(Toggle toggle) {
        this.toggle = toggle;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
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
