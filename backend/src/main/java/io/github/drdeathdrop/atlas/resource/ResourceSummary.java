package io.github.drdeathdrop.atlas.resource;

import java.util.UUID;

public record ResourceSummary(
        UUID id,
        String callSign,
        ResourceKind kind,
        ResourceType type,
        ResourceStatus status,
        double latitude,
        double longitude,
        UUID teamId
) {
}
