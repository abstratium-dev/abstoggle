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
@Table(name = "T_toggle_criterion")
@Audited
public class ToggleCriterion {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "toggle_rule_id",
        nullable = false,
        foreignKey = @jakarta.persistence.ForeignKey(name = "FK_toggle_criterion_rule_id")
    )
    private ToggleRule toggleRule;

    @Column(name = "criterion_key", length = 100, nullable = false)
    private String criterionKey;

    @Column(name = "criterion_value", length = 500, nullable = false)
    private String criterionValue;

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

    public ToggleRule getToggleRule() {
        return toggleRule;
    }

    public void setToggleRule(ToggleRule toggleRule) {
        this.toggleRule = toggleRule;
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
}
