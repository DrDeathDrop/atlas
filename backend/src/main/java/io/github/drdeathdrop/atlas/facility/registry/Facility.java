package io.github.drdeathdrop.atlas.facility.registry;

import io.github.drdeathdrop.atlas.facility.FacilityType;
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
@Table(name = "facilities")
public class Facility {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false, unique = true, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 16)
    private FacilityType type;

    @Column(length = 255)
    private String address;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private int occupancy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected Facility() {
    }

    public Facility(String name, FacilityType type, String address, Point location, int capacity) {
        this.name = name;
        this.type = type;
        this.address = address;
        this.location = location;
        this.capacity = capacity;
        this.occupancy = 0;
    }

    public void changeOccupancy(int newOccupancy) {
        this.occupancy = newOccupancy;
    }
}
