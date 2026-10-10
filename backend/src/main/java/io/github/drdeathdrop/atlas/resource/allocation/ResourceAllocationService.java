package io.github.drdeathdrop.atlas.resource.allocation;

import io.github.drdeathdrop.atlas.resource.NearbyResource;
import io.github.drdeathdrop.atlas.resource.ResourceAllocation;
import io.github.drdeathdrop.atlas.resource.ResourceStatus;
import io.github.drdeathdrop.atlas.resource.ResourceSummary;
import io.github.drdeathdrop.atlas.resource.ResourceType;
import io.github.drdeathdrop.atlas.resource.inventory.NearbyResourceRow;
import io.github.drdeathdrop.atlas.resource.inventory.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ResourceAllocationService implements ResourceAllocation {
    private static final double METERS_PER_KILOMETER = 1000.0;

    private final ResourceRepository repository;

    public ResourceAllocationService(ResourceRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NearbyResource> findAvailableNear(double latitude, double longitude, double radiusKm,
                                                  ResourceType type) {
        if (radiusKm <= 0) {
            throw new IllegalArgumentException("The search radius must be greater than zero");
        }

        String typeName = type == null ? null : type.name();

        return repository.findAvailableNear(latitude, longitude, radiusKm * METERS_PER_KILOMETER, typeName).stream()
                .map(ResourceAllocationService::toNearbyResource)
                .toList();
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
