package io.github.drdeathdrop.atlas.incident.core;

import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull IncidentStatus status) {
}
