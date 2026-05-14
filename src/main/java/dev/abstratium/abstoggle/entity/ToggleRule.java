package dev.abstratium.abstoggle.entity;

import java.util.UUID;

import org.hibernate.envers.Audited;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "T_toggle_rule",
    uniqueConstraints = @UniqueConstraint(
        name = "UQ_toggle_rule_name",
        columnNames = {"name"}
    )
)
@Audited
public class ToggleRule {

    @Id
    @Column(length = 36)
    private String id;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(name = "rule_value", length = 255, nullable = false)
    private String ruleValue = "off";

    @Column(length = 500)
    private String description;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
