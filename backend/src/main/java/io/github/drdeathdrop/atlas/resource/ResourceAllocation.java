package io.github.drdeathdrop.atlas.resource;

import io.github.drdeathdrop.atlas.user.Role;

import java.util.List;
import java.util.UUID;

public interface ResourceAllocation {
    List<NearbyResource> findAvailableNear(double latitude, double longitude, double radiusKm, ResourceType type);

    AssignmentSummary assign(UUID resourceId, UUID incidentId, UUID assignedBy, Role role);

    AssignmentSummary release(UUID assignmentId, UUID releasedBy, Role role);

    List<AssignmentSummary> releaseAllFor(UUID incidentId, UUID releasedBy, Role role);

    List<AssignmentSummary> assignmentsFor(UUID incidentId);
}
