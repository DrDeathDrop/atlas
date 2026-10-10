package io.github.drdeathdrop.atlas.audit.log;

import io.github.drdeathdrop.atlas.incident.IncidentReported;
import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.incident.IncidentStatusChanged;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class IncidentAuditListener {
    private final AuditEntryRepository repository;

    public IncidentAuditListener(AuditEntryRepository repository) {
        this.repository = repository;
    }

    @EventListener
    public void on(IncidentReported event) {
        repository.save(new AuditEntry(
                event.reportedBy(),
                event.reporterRole(),
                AuditAction.INCIDENT_REPORTED,
                AuditEntry.INCIDENT,
                event.incidentId(),
                event.reference(),
                null,
                IncidentStatus.REPORTED.name()));
    }

    @EventListener
    public void on(IncidentStatusChanged event) {
        repository.save(new AuditEntry(
                event.changedBy(),
                event.changedByRole(),
                AuditAction.INCIDENT_STATUS_CHANGED,
                AuditEntry.INCIDENT,
                event.incidentId(),
                event.reference(),
                event.from().name(),
                event.to().name()));
    }
}
