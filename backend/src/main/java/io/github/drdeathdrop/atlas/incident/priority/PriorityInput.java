package io.github.drdeathdrop.atlas.incident.priority;

import io.github.drdeathdrop.atlas.incident.IncidentCategory;
import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.incident.Severity;

import java.time.Instant;
import java.util.UUID;

public record PriorityInput(
        UUID incidentId,
        Severity severity,
        IncidentCategory category,
        IncidentStatus status,
        int affectedPeople,
        Instant reportedAt,
        boolean hasResources
) {
}
