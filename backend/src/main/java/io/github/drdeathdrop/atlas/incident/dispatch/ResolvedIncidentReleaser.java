package io.github.drdeathdrop.atlas.incident.dispatch;

import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.incident.IncidentStatusChanged;
import io.github.drdeathdrop.atlas.resource.ResourceAllocation;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ResolvedIncidentReleaser {
    private final ResourceAllocation resourceAllocation;

    public ResolvedIncidentReleaser(ResourceAllocation resourceAllocation) {
        this.resourceAllocation = resourceAllocation;
    }

    @EventListener
    public void on(IncidentStatusChanged event) {
        if (event.to() == IncidentStatus.RESOLVED) {
            resourceAllocation.releaseAllFor(event.incidentId(), event.changedBy(), event.changedByRole());
        }
    }
}
