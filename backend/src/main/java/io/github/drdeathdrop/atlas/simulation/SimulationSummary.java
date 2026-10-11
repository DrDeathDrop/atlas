package io.github.drdeathdrop.atlas.simulation;

import java.time.Instant;
import java.util.UUID;

public record SimulationSummary(
        UUID id,
        Scenario scenario,
        double latitude,
        double longitude,
        double radiusKm,
        int planned,
        int created,
        int intervalSeconds,
        boolean relatedIncidents,
        SimulationStatus status,
        Instant startedAt,
        Instant finishedAt
) {
}
