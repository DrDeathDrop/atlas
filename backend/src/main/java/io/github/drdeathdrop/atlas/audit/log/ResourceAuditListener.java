package io.github.drdeathdrop.atlas.audit.log;

import io.github.drdeathdrop.atlas.resource.ResourceAssigned;
import io.github.drdeathdrop.atlas.resource.ResourceReleased;
import io.github.drdeathdrop.atlas.resource.ResourceStatus;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ResourceAuditListener {
    private final AuditEntryRepository repository;

    public ResourceAuditListener(AuditEntryRepository repository) {
        this.repository = repository;
    }

    @EventListener
    public void on(ResourceAssigned event) {
        repository.save(new AuditEntry(
                event.assignedBy(),
                event.assignedByRole(),
                AuditAction.RESOURCE_ASSIGNED,
                AuditEntry.RESOURCE,
                event.resourceId(),
                event.callSign(),
                ResourceStatus.AVAILABLE.name(),
                ResourceStatus.EN_ROUTE.name(),
                event.incidentId()));
    }

    @EventListener
    public void on(ResourceReleased event) {
        repository.save(new AuditEntry(
                event.releasedBy(),
                event.releasedByRole(),
                AuditAction.RESOURCE_RELEASED,
                AuditEntry.RESOURCE,
                event.resourceId(),
                event.callSign(),
                event.previousStatus().name(),
                ResourceStatus.AVAILABLE.name(),
                event.incidentId()));
    }
}
