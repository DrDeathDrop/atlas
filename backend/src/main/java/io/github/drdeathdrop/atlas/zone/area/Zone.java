package io.github.drdeathdrop.atlas.zone.area;

import io.github.drdeathdrop.atlas.zone.ZoneType;
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
import org.locationtech.jts.geom.Polygon;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "zones")
public class Zone {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 16)
    private ZoneType type;

    @Column(nullable = false, updatable = false, columnDefinition = "geometry(Polygon,4326)")
    private Polygon boundary;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private UUID liftedBy;

    private Instant liftedAt;

    protected Zone() {
    }

    public Zone(String name, ZoneType type, Polygon boundary, UUID createdBy) {
        this.name = name;
        this.type = type;
        this.boundary = boundary;
        this.createdBy = createdBy;
    }

    public boolean isLifted() {
        return liftedAt != null;
    }

    public void lift(UUID by, Instant at) {
        this.liftedBy = by;
        this.liftedAt = at;
    }
}
