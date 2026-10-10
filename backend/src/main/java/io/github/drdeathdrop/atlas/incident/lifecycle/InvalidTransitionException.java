package io.github.drdeathdrop.atlas.incident.lifecycle;

import io.github.drdeathdrop.atlas.incident.IncidentStatus;

public class InvalidTransitionException extends RuntimeException {
    public InvalidTransitionException(IncidentStatus from, IncidentStatus to) {
        super("An incident cannot move from " + from + " to " + to);
    }
}
