package io.github.drdeathdrop.atlas.resource;

import io.github.drdeathdrop.atlas.user.Role;

import java.util.UUID;

public record ResourceAssigned(UUID assignmentId, UUID resourceId, String callSign, UUID incidentId,
                               UUID assignedBy, Role assignedByRole) {
}
