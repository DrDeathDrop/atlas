package io.github.drdeathdrop.atlas.incident;

import io.github.drdeathdrop.atlas.user.Role;

import java.util.UUID;

public record IncidentReported(UUID incidentId, String reference, UUID reportedBy, Role reporterRole) {
}
