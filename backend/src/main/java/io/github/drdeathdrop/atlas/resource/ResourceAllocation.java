package io.github.drdeathdrop.atlas.resource;

import java.util.List;

public interface ResourceAllocation {
    List<NearbyResource> findAvailableNear(double latitude, double longitude, double radiusKm, ResourceType type);
}
