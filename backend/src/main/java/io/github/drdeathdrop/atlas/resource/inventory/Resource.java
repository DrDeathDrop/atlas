package io.github.drdeathdrop.atlas.resource.inventory;

import io.github.drdeathdrop.atlas.resource.ResourceStatus;
import io.github.drdeathdrop.atlas.resource.ResourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "resources")
public class Resource {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false, unique = true, length = 64)
    private String callSign;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 32)
    private ResourceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ResourceStatus status;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point location;

    private UUID teamId;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected Resource() {
    }

    public Resource(String callSign, ResourceType type, Point location, UUID teamId) {
        this.callSign = callSign;
        this.type = type;
        this.location = location;
        this.teamId = teamId;
        this.status = ResourceStatus.AVAILABLE;
    }

    public void relocate(Point newLocation) {
        this.location = newLocation;
    }

    public void changeStatus(ResourceStatus newStatus) {
        this.status = newStatus;
    }
}
