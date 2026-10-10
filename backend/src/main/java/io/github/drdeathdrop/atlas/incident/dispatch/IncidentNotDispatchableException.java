package io.github.drdeathdrop.atlas.incident.dispatch;

import io.github.drdeathdrop.atlas.incident.IncidentStatus;

public class IncidentNotDispatchableException extends RuntimeException {
    public IncidentNotDispatchableException(String reference, IncidentStatus status) {
        super("Resources cannot be sent to " + reference + " while it is " + status);
    }
}
