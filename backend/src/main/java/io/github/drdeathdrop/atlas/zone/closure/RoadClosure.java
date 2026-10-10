package io.github.drdeathdrop.atlas.zone.closure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.LineString;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "road_closures")
public class RoadClosure {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false, length = 120)
    private String roadName;

    @Column(updatable = false, length = 255)
    private String reason;

    @Column(nullable = false, updatable = false, columnDefinition = "geometry(LineString,4326)")
    private LineString path;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private UUID reopenedBy;

    private Instant reopenedAt;

    protected RoadClosure() {
    }

    public RoadClosure(String roadName, String reason, LineString path, UUID createdBy) {
        this.roadName = roadName;
        this.reason = reason;
        this.path = path;
        this.createdBy = createdBy;
    }

    public boolean isReopened() {
        return reopenedAt != null;
    }

    public void reopen(UUID by, Instant at) {
        this.reopenedBy = by;
        this.reopenedAt = at;
    }
}
