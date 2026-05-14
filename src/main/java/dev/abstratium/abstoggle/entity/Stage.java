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
 * Represents a deployment stage (e.g., "dev", "test", "prod") that can participate
 * in a parent-child inheritance chain for toggle resolution.
 */
@Entity
@Table(name = "T_stage")
@Audited
public class Stage {

    /**
     * v4 UUID generated in Java code.
     */
    @Id
    @Column(length = 36)
    private String id;

    /**
     * Stage identifier (e.g., "dev", "test", "prod"). Must be unique.
     */
    @Column(length = 100, nullable = false, unique = true)
    private String name;

    /**
     * Human-readable description of this stage.
     */
    @Column(length = 500)
    private String description;

    /**
     * UI presentation order for this stage.
     */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    /**
     * Optional parent stage for inheritance chain.
     * When querying toggles for a stage, if a toggle is not defined for that stage,
     * the system walks up the inheritance chain looking for the toggle by name.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_stage_id", foreignKey = @jakarta.persistence.ForeignKey(name = "FK_stage_parent_stage_id"))
    private Stage parentStage;

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

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Stage getParentStage() {
        return parentStage;
    }

    public void setParentStage(Stage parentStage) {
        this.parentStage = parentStage;
    }
}
