package io.github.drdeathdrop.atlas.facility;

import java.util.UUID;

public record FacilitySummary(
        UUID id,
        String name,
        FacilityType type,
        String address,
        double latitude,
        double longitude,
        int capacity,
        int occupancy
) {
}
