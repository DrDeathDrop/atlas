package io.github.drdeathdrop.atlas.resource;

import io.github.drdeathdrop.atlas.user.Role;

import java.util.UUID;

public record ResourceReleased(UUID assignmentId, UUID resourceId, String callSign, UUID incidentId,
                               ResourceStatus previousStatus, UUID releasedBy, Role releasedByRole) {
}
