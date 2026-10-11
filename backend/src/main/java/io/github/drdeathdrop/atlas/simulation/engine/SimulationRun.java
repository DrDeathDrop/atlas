package io.github.drdeathdrop.atlas.simulation.engine;

import io.github.drdeathdrop.atlas.simulation.Scenario;
import io.github.drdeathdrop.atlas.simulation.SimulationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "simulation_runs")
public class SimulationRun {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 32)
    private Scenario scenario;

    @Column(nullable = false, updatable = false)
    private double latitude;

    @Column(nullable = false, updatable = false)
    private double longitude;

    @Column(nullable = false, updatable = false)
    private double radiusKm;

    @Column(nullable = false, updatable = false)
    private int planned;

    @Column(nullable = false)
    private int created;

    @Column(nullable = false, updatable = false)
    private int intervalSeconds;

    @Column(nullable = false, updatable = false)
    private boolean relatedIncidents;

    @Column(nullable = false, updatable = false)
    private long seed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SimulationStatus status;

    @Column(nullable = false, updatable = false)
    private UUID startedBy;

    @Column(nullable = false, updatable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant nextIncidentAt;

    private Instant finishedAt;

    protected SimulationRun() {
    }

    public SimulationRun(Scenario scenario, double latitude, double longitude, double radiusKm, int planned,
                         int intervalSeconds, boolean relatedIncidents, long seed, UUID startedBy,
                         Instant startedAt) {
        this.scenario = scenario;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusKm = radiusKm;
        this.planned = planned;
        this.intervalSeconds = intervalSeconds;
        this.relatedIncidents = relatedIncidents;
        this.seed = seed;
        this.startedBy = startedBy;
        this.startedAt = startedAt;
        this.nextIncidentAt = startedAt;
        this.created = 0;
        this.status = SimulationStatus.RUNNING;
    }

    public boolean isRunning() {
        return status == SimulationStatus.RUNNING;
    }

    public void recordIncident(Instant at) {
        created++;
        if (created >= planned) {
            status = SimulationStatus.COMPLETED;
            finishedAt = at;
        } else {
            nextIncidentAt = at.plusSeconds(intervalSeconds);
        }
    }

    public boolean isStopped() {
        return status == SimulationStatus.STOPPED;
    }

    public void stop(Instant at) {
        status = SimulationStatus.STOPPED;
        finishedAt = at;
    }

    public void resume(Instant at) {
        status = SimulationStatus.RUNNING;
        finishedAt = null;
        nextIncidentAt = at;
    }
}
