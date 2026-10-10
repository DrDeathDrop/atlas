package io.github.drdeathdrop.atlas.incident;

import java.util.UUID;

public record IncidentReported(UUID incidentId, String reference, UUID reportedBy) {
}
