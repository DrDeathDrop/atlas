package io.github.drdeathdrop.atlas.incident;

import io.github.drdeathdrop.atlas.user.Role;

import java.util.UUID;

public record IncidentStatusChanged(UUID incidentId, String reference, IncidentStatus from, IncidentStatus to,
                                    UUID changedBy, Role changedByRole) {
}
