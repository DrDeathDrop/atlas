package io.github.drdeathdrop.atlas.messaging.outbox;

import io.github.drdeathdrop.atlas.incident.IncidentReported;
import io.github.drdeathdrop.atlas.incident.IncidentStatusChanged;
import io.github.drdeathdrop.atlas.messaging.IntegrationEvent;
import io.github.drdeathdrop.atlas.resource.ResourceAssigned;
import io.github.drdeathdrop.atlas.resource.ResourceReleased;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Component
public class OutboxWriter {
    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxWriter(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(IncidentReported event) {
        store(new IntegrationEvent(UUID.randomUUID(), IntegrationEvent.INCIDENT_REPORTED,
                event.incidentId(), event.reference(), null, null, null, event.reportedBy()));
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(IncidentStatusChanged event) {
        store(new IntegrationEvent(UUID.randomUUID(), IntegrationEvent.INCIDENT_STATUS_CHANGED,
                event.incidentId(), event.reference(), event.to().name(), null, null, event.changedBy()));
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(ResourceAssigned event) {
        store(new IntegrationEvent(UUID.randomUUID(), IntegrationEvent.RESOURCE_ASSIGNED,
                event.incidentId(), null, null, event.resourceId(), event.callSign(), event.assignedBy()));
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(ResourceReleased event) {
        store(new IntegrationEvent(UUID.randomUUID(), IntegrationEvent.RESOURCE_RELEASED,
                event.incidentId(), null, null, event.resourceId(), event.callSign(), event.releasedBy()));
    }

    private void store(IntegrationEvent event) {
        repository.save(new OutboxEvent(
                event.eventId(),
                event.type(),
                objectMapper.writeValueAsString(event),
                Instant.now()));
    }
}
