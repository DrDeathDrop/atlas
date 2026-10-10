package io.github.drdeathdrop.atlas.zone;

import io.github.drdeathdrop.atlas.shared.geo.GeoPosition;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoadClosureSummary(
        UUID id,
        String roadName,
        String reason,
        List<GeoPosition> path,
        Instant createdAt,
        Instant reopenedAt
) {
}
