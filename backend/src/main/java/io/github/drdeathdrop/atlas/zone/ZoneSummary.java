package io.github.drdeathdrop.atlas.zone;

import io.github.drdeathdrop.atlas.shared.geo.GeoPosition;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ZoneSummary(
        UUID id,
        String name,
        ZoneType type,
        List<GeoPosition> boundary,
        Instant createdAt,
        Instant liftedAt
) {
}
