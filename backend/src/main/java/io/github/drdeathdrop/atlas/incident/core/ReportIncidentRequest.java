package io.github.drdeathdrop.atlas.incident.core;

import io.github.drdeathdrop.atlas.incident.IncidentCategory;
import io.github.drdeathdrop.atlas.incident.Severity;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportIncidentRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 5000) String description,
        @NotNull IncidentCategory category,
        @NotNull Severity severity,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @Min(0) int affectedPeople
) {
}
