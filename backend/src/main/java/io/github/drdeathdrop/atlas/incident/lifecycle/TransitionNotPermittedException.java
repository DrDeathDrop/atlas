package io.github.drdeathdrop.atlas.incident.lifecycle;

import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.user.Role;

public class TransitionNotPermittedException extends RuntimeException {
    public TransitionNotPermittedException(Role role, IncidentStatus from, IncidentStatus to) {
        super("Role " + role + " may not move an incident from " + from + " to " + to);
    }
}
