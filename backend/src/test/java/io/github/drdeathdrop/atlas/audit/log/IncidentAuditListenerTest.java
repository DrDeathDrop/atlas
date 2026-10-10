package io.github.drdeathdrop.atlas.audit.log;

import io.github.drdeathdrop.atlas.incident.IncidentReported;
import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.incident.IncidentStatusChanged;
import io.github.drdeathdrop.atlas.user.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IncidentAuditListenerTest {

    @Mock
    private AuditEntryRepository repository;

    @InjectMocks
    private IncidentAuditListener listener;

    @Test
    void recordsAReportedIncident() {
        UUID incidentId = UUID.randomUUID();
        UUID reporter = UUID.randomUUID();

        listener.on(new IncidentReported(incidentId, "INC-2026-0001", reporter, Role.DISPATCHER));

        AuditEntry entry = savedEntry();
        assertThat(entry.getAction()).isEqualTo(AuditAction.INCIDENT_REPORTED);
        assertThat(entry.getEntityType()).isEqualTo(AuditEntry.INCIDENT);
        assertThat(entry.getEntityId()).isEqualTo(incidentId);
        assertThat(entry.getEntityReference()).isEqualTo("INC-2026-0001");
        assertThat(entry.getActorId()).isEqualTo(reporter);
        assertThat(entry.getActorRole()).isEqualTo(Role.DISPATCHER);
        assertThat(entry.getPreviousState()).isNull();
        assertThat(entry.getNewState()).isEqualTo("REPORTED");
        assertThat(entry.getOccurredAt()).isNotNull();
    }

    @Test
    void recordsAStatusChangeWithBothStates() {
        UUID incidentId = UUID.randomUUID();
        UUID operator = UUID.randomUUID();

        listener.on(new IncidentStatusChanged(incidentId, "INC-2026-0001",
                IncidentStatus.ACTIVE, IncidentStatus.CONTAINED, operator, Role.FIELD_OPERATOR));

        AuditEntry entry = savedEntry();
        assertThat(entry.getAction()).isEqualTo(AuditAction.INCIDENT_STATUS_CHANGED);
        assertThat(entry.getEntityType()).isEqualTo(AuditEntry.INCIDENT);
        assertThat(entry.getEntityId()).isEqualTo(incidentId);
        assertThat(entry.getEntityReference()).isEqualTo("INC-2026-0001");
        assertThat(entry.getActorId()).isEqualTo(operator);
        assertThat(entry.getActorRole()).isEqualTo(Role.FIELD_OPERATOR);
        assertThat(entry.getPreviousState()).isEqualTo("ACTIVE");
        assertThat(entry.getNewState()).isEqualTo("CONTAINED");
    }

    private AuditEntry savedEntry() {
        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }
}
