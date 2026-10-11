package io.github.drdeathdrop.atlas.simulation.engine;

import io.github.drdeathdrop.atlas.simulation.Scenario;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StartSimulationRequest(
        @NotNull Scenario scenario,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @NotNull @DecimalMin("0.5") @DecimalMax("100") Double radiusKm,
        @NotNull @Min(1) @Max(50) Integer incidents,
        @NotNull @Min(1) @Max(120) Integer intervalSeconds,
        Boolean relatedIncidents,
        Long seed
) {
}
