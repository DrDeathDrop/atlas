package io.github.drdeathdrop.atlas.incident;

import java.util.List;
import java.util.UUID;

public record Priority(UUID incidentId, int score, PriorityLevel level, List<PriorityFactor> factors) {
}
