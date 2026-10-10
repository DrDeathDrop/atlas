package io.github.drdeathdrop.atlas.resource.inventory;

import io.github.drdeathdrop.atlas.resource.ResourceType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateResourceRequest(
        @NotBlank @Size(max = 64) String callSign,
        @NotNull ResourceType type,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
        UUID teamId
) {
}
