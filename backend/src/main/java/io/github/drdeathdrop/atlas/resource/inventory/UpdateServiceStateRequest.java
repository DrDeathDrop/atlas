package io.github.drdeathdrop.atlas.resource.inventory;

import jakarta.validation.constraints.NotNull;

public record UpdateServiceStateRequest(@NotNull Boolean inService) {
}
