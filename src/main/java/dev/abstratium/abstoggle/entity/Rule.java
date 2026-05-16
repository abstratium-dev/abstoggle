package dev.abstratium.abstoggle.entity;

import java.util.UUID;

import org.hibernate.envers.Audited;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Represents a reusable rule that defines a set of criteria for matching
 * against client context. Enables OR logic by allowing multiple rules
 * for the same toggle. The actual value is set on the assignment
 * ({@link ToggleStageRule}), not on the rule itself.
 */
@Entity
@Table(
    name = "T_rule",
    uniqueConstraints = @UniqueConstraint(
        name = "UQ_rule_name",
        columnNames = {"name"}
    )
)
@Audited
public class Rule {

    /**
     * v4 UUID generated in Java code.
     */
    @Id
    @Column(length = 36)
    private String id;

    /**
     * Unique identifier so rules can be picked and reused across toggles.
     */
    @Column(length = 255, nullable = false)
    private String name;

    /**
     * Human-readable description of this rule.
     */
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
