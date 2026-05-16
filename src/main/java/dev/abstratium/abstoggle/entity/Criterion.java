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

/**
 * Represents a single criterion within a {@link Rule}, consisting of a
 * key and a regex pattern value used for client-side matching.
 */
@Entity
@Table(name = "T_criterion")
@Audited
public class Criterion {

    /**
     * v4 UUID generated in Java code.
     */
    @Id
    @Column(length = 36)
    private String id;

    /**
     * Reference to the parent {@link Rule} this criterion belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "rule_id",
        nullable = false,
        foreignKey = @jakarta.persistence.ForeignKey(name = "FK_criterion_rule_id")
    )
    private Rule rule;

    /**
     * Key for matching (e.g., "userId", "country").
     */
    @Column(name = "criterion_key", length = 100, nullable = false)
    private String criterionKey;

    /**
     * Regex pattern for matching (e.g., "/10.../", "DE").
     */
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

    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
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
