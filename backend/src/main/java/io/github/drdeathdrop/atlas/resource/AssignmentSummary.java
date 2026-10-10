package io.github.drdeathdrop.atlas.resource;

import java.time.Instant;
import java.util.UUID;

public record AssignmentSummary(
        UUID id,
        UUID resourceId,
        String callSign,
        UUID incidentId,
        Instant assignedAt,
        Instant releasedAt
) {
}
