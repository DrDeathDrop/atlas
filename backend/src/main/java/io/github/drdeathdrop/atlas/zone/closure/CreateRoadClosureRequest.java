package io.github.drdeathdrop.atlas.zone.closure;

import io.github.drdeathdrop.atlas.shared.geo.GeoPosition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateRoadClosureRequest(
        @NotBlank @Size(max = 120) String roadName,
        @Size(max = 255) String reason,
        @NotNull @Size(min = 2, max = 200) List<@NotNull @Valid GeoPosition> path
) {
}
