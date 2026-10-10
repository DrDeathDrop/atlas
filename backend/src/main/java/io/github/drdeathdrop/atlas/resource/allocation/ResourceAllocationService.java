package io.github.drdeathdrop.atlas.resource.allocation;

import io.github.drdeathdrop.atlas.resource.AssignmentSummary;
import io.github.drdeathdrop.atlas.resource.NearbyResource;
import io.github.drdeathdrop.atlas.resource.ResourceAllocation;
import io.github.drdeathdrop.atlas.resource.ResourceAssigned;
import io.github.drdeathdrop.atlas.resource.ResourceReleased;
import io.github.drdeathdrop.atlas.resource.ResourceStatus;
import io.github.drdeathdrop.atlas.resource.ResourceSummary;
import io.github.drdeathdrop.atlas.resource.ResourceType;
import io.github.drdeathdrop.atlas.resource.inventory.NearbyResourceRow;
import io.github.drdeathdrop.atlas.resource.inventory.Resource;
import io.github.drdeathdrop.atlas.resource.inventory.ResourceNotFoundException;
import io.github.drdeathdrop.atlas.resource.inventory.ResourceRepository;
import io.github.drdeathdrop.atlas.user.Role;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ResourceAllocationService implements ResourceAllocation {
    private static final double METERS_PER_KILOMETER = 1000.0;

    private final ResourceRepository resourceRepository;
    private final AssignmentRepository assignmentRepository;
    private final ApplicationEventPublisher events;

    public ResourceAllocationService(ResourceRepository resourceRepository,
                                     AssignmentRepository assignmentRepository,
                                     ApplicationEventPublisher events) {
        this.resourceRepository = resourceRepository;
        this.assignmentRepository = assignmentRepository;
        this.events = events;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NearbyResource> findAvailableNear(double latitude, double longitude, double radiusKm,
                                                  ResourceType type) {
        if (radiusKm <= 0) {
            throw new IllegalArgumentException("The search radius must be greater than zero");
        }

        String typeName = type == null ? null : type.name();

        return resourceRepository
                .findAvailableNear(latitude, longitude, radiusKm * METERS_PER_KILOMETER, typeName).stream()
                .map(ResourceAllocationService::toNearbyResource)
                .toList();
    }

    @Override
    @Transactional
    public AssignmentSummary assign(UUID resourceId, UUID incidentId, UUID assignedBy, Role role) {
        Resource resource = resourceRepository.findByIdForUpdate(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));

        if (resource.getStatus() != ResourceStatus.AVAILABLE) {
            throw new ResourceNotAvailableException(resource.getCallSign(), resource.getStatus());
        }

        resource.changeStatus(ResourceStatus.EN_ROUTE);

        Assignment assignment = assignmentRepository.saveAndFlush(
                new Assignment(resourceId, incidentId, assignedBy));

        events.publishEvent(new ResourceAssigned(
                assignment.getId(), resourceId, resource.getCallSign(), incidentId, assignedBy, role));

        return toSummary(assignment, resource.getCallSign());
    }

    @Override
    @Transactional
    public AssignmentSummary release(UUID assignmentId, UUID releasedBy, Role role) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException(assignmentId));

        if (!assignment.isOpen()) {
            throw new AssignmentAlreadyReleasedException(assignmentId);
        }

        Resource resource = resourceRepository.findByIdForUpdate(assignment.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException(assignment.getResourceId()));

        ResourceStatus previousStatus = resource.getStatus();

        assignment.release(releasedBy);
        resource.changeStatus(ResourceStatus.AVAILABLE);

        events.publishEvent(new ResourceReleased(
                assignment.getId(), assignment.getResourceId(), resource.getCallSign(), assignment.getIncidentId(),
                previousStatus, releasedBy, role));

        return toSummary(assignment, resource.getCallSign());
    }

    @Override
    @Transactional
    public List<AssignmentSummary> releaseAllFor(UUID incidentId, UUID releasedBy, Role role) {
        List<AssignmentSummary> released = new ArrayList<>();
        for (Assignment assignment : assignmentRepository.findByIncidentIdAndReleasedAtIsNull(incidentId)) {
            released.add(release(assignment.getId(), releasedBy, role));
        }
        return released;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentSummary> assignmentsFor(UUID incidentId) {
        return assignmentRepository.findByIncidentIdOrderByAssignedAtAsc(incidentId).stream()
                .map(assignment -> toSummary(assignment, callSignOf(assignment.getResourceId())))
                .toList();
    }

    static AssignmentSummary toSummary(Assignment assignment, String callSign) {
        return new AssignmentSummary(
                assignment.getId(),
                assignment.getResourceId(),
                callSign,
                assignment.getIncidentId(),
                assignment.getAssignedAt(),
                assignment.getReleasedAt());
    }

    private String callSignOf(UUID resourceId) {
        return resourceRepository.findById(resourceId).map(Resource::getCallSign).orElse(null);
    }

    private static NearbyResource toNearbyResource(NearbyResourceRow row) {
        ResourceType type = ResourceType.valueOf(row.getType());
        ResourceSummary summary = new ResourceSummary(
                row.getId(),
                row.getCallSign(),
                type.kind(),
                type,
                ResourceStatus.valueOf(row.getStatus()),
                row.getLatitude(),
                row.getLongitude(),
                row.getTeamId());
        return new NearbyResource(summary, row.getDistanceMeters());
    }
}
