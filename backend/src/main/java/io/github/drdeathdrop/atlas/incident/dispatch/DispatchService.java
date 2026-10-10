package io.github.drdeathdrop.atlas.incident.dispatch;

import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.incident.IncidentSummary;
import io.github.drdeathdrop.atlas.incident.core.IncidentService;
import io.github.drdeathdrop.atlas.resource.AssignmentSummary;
import io.github.drdeathdrop.atlas.resource.ResourceAllocation;
import io.github.drdeathdrop.atlas.user.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DispatchService {
    private final IncidentService incidentService;
    private final ResourceAllocation resourceAllocation;

    public DispatchService(IncidentService incidentService, ResourceAllocation resourceAllocation) {
        this.incidentService = incidentService;
        this.resourceAllocation = resourceAllocation;
    }

    @Transactional
    public AssignmentSummary dispatch(UUID incidentId, UUID resourceId, UUID dispatchedBy, Role role) {
        IncidentSummary incident = incidentService.get(incidentId);

        if (incident.status() != IncidentStatus.VERIFIED && incident.status() != IncidentStatus.ACTIVE) {
            throw new IncidentNotDispatchableException(incident.reference(), incident.status());
        }

        AssignmentSummary assignment = resourceAllocation.assign(resourceId, incidentId, dispatchedBy, role);

        if (incident.status() == IncidentStatus.VERIFIED) {
            incidentService.changeStatus(incidentId, IncidentStatus.ACTIVE, dispatchedBy, role);
        }

        return assignment;
    }
}
