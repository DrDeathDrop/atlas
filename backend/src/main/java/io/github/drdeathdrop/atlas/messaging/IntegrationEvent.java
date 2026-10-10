package io.github.drdeathdrop.atlas.messaging;

import java.util.UUID;

public record IntegrationEvent(
        UUID eventId,
        String type,
        UUID incidentId,
        String incidentReference,
        String incidentStatus,
        UUID resourceId,
        String callSign,
        UUID actorId
) {
    public static final String INCIDENT_REPORTED = "incident.reported";
    public static final String INCIDENT_STATUS_CHANGED = "incident.status-changed";
    public static final String RESOURCE_ASSIGNED = "resource.assigned";
    public static final String RESOURCE_RELEASED = "resource.released";
}
