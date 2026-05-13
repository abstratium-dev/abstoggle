package dev.abstratium.abstoggle.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
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

@Entity
@Table(
    name = "T_toggle_stage",
    uniqueConstraints = @UniqueConstraint(
        name = "UQ_toggle_stage_toggle_stage",
        columnNames = {"toggle_id", "stage_id"}
    )
)
@Audited
public class ToggleStage {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "toggle_id",
        nullable = false,
        foreignKey = @jakarta.persistence.ForeignKey(name = "FK_toggle_stage_toggle_id")
    )
    private Toggle toggle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "stage_id",
        nullable = false,
        foreignKey = @jakarta.persistence.ForeignKey(name = "FK_toggle_stage_stage_id")
    )
    private Stage stage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
