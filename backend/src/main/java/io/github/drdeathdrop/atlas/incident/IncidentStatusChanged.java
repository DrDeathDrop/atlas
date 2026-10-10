package io.github.drdeathdrop.atlas.incident;

import java.util.UUID;

public record IncidentStatusChanged(UUID incidentId, String reference, IncidentStatus from, IncidentStatus to,
                                    UUID changedBy) {
}
