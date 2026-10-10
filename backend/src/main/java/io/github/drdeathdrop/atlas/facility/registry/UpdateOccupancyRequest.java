package io.github.drdeathdrop.atlas.facility.registry;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateOccupancyRequest(@NotNull @Min(0) Integer occupancy) {
}
