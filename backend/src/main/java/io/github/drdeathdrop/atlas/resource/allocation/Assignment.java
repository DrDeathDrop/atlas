package io.github.drdeathdrop.atlas.resource.allocation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "assignments")
public class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID resourceId;

    @Column(nullable = false, updatable = false)
    private UUID incidentId;

    @Column(nullable = false, updatable = false)
    private UUID assignedBy;

    @Column(nullable = false, updatable = false)
    private Instant assignedAt;

    private UUID releasedBy;

    private Instant releasedAt;

    protected Assignment() {
    }

    public Assignment(UUID resourceId, UUID incidentId, UUID assignedBy) {
        this.resourceId = resourceId;
        this.incidentId = incidentId;
        this.assignedBy = assignedBy;
        this.assignedAt = Instant.now();
    }

    public boolean isOpen() {
        return releasedAt == null;
    }

    public void release(UUID releasedBy) {
        this.releasedBy = releasedBy;
        this.releasedAt = Instant.now();
    }
}
