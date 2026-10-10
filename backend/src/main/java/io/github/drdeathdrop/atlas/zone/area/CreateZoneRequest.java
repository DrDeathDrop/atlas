package io.github.drdeathdrop.atlas.zone.area;

import io.github.drdeathdrop.atlas.shared.geo.GeoPosition;
import io.github.drdeathdrop.atlas.zone.ZoneType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateZoneRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull ZoneType type,
        @NotNull @Size(min = 3, max = 200) List<@NotNull @Valid GeoPosition> boundary
) {
}
