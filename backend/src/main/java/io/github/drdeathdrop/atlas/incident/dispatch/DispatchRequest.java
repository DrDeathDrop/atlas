package io.github.drdeathdrop.atlas.incident.dispatch;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DispatchRequest(@NotNull UUID resourceId) {
}
