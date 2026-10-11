package io.github.drdeathdrop.atlas.incident;

import io.github.drdeathdrop.atlas.user.Role;

import java.util.UUID;

public interface IncidentIntake {
    IncidentSummary report(NewIncident incident, UUID reportedBy, Role role);
}
