package io.github.drdeathdrop.atlas.incident;

import java.time.Instant;
import java.util.UUID;

public record IncidentSummary(
        UUID id,
        String reference,
        String title,
        String description,
        IncidentCategory category,
        Severity severity,
        IncidentStatus status,
        double latitude,
        double longitude,
        int affectedPeople,
        UUID reportedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
