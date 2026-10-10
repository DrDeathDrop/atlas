package io.github.drdeathdrop.atlas.incident.core;

import io.github.drdeathdrop.atlas.incident.IncidentCategory;
import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.incident.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "incidents")
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false, unique = true, length = 32)
    private String reference;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IncidentCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private IncidentStatus status;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Column(nullable = false)
    private int affectedPeople;

    @Column(nullable = false, updatable = false)
    private UUID reportedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected Incident() {
    }

    public Incident(String reference, String title, String description, IncidentCategory category,
                    Severity severity, Point location, int affectedPeople, UUID reportedBy) {
        this.reference = reference;
        this.title = title;
        this.description = description;
        this.category = category;
        this.severity = severity;
        this.location = location;
        this.affectedPeople = affectedPeople;
        this.reportedBy = reportedBy;
        this.status = IncidentStatus.REPORTED;
    }

    public void moveTo(IncidentStatus newStatus) {
        this.status = newStatus;
    }
}
