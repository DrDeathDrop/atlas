package io.github.drdeathdrop.atlas.messaging.outbox;

import io.github.drdeathdrop.atlas.incident.IncidentReported;
import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.incident.IncidentStatusChanged;
import io.github.drdeathdrop.atlas.messaging.IntegrationEvent;
import io.github.drdeathdrop.atlas.resource.ResourceAssigned;
import io.github.drdeathdrop.atlas.resource.ResourceReleased;
import io.github.drdeathdrop.atlas.resource.ResourceStatus;
import io.github.drdeathdrop.atlas.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxWriterTest {

    private static final UUID INCIDENT = UUID.randomUUID();
    private static final UUID RESOURCE = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Mock
    private OutboxEventRepository repository;

    private OutboxWriter writer;

    @BeforeEach
    void setUp() {
        writer = new OutboxWriter(repository, objectMapper);
    }

    @Test
    void aReportedIncidentIsStoredForPublishing() {
        writer.on(new IncidentReported(INCIDENT, "INC-2026-0001", USER, Role.DISPATCHER));

        OutboxEvent stored = stored();
        IntegrationEvent event = payloadOf(stored);

        assertThat(stored.getRoutingKey()).isEqualTo("incident.reported");
        assertThat(stored.getId()).isEqualTo(event.eventId());
        assertThat(stored.getCreatedAt()).isNotNull();
        assertThat(stored.getPublishedAt()).isNull();
        assertThat(event.type()).isEqualTo(IntegrationEvent.INCIDENT_REPORTED);
        assertThat(event.incidentId()).isEqualTo(INCIDENT);
        assertThat(event.incidentReference()).isEqualTo("INC-2026-0001");
        assertThat(event.actorId()).isEqualTo(USER);
    }

    @Test
    void aStatusChangeCarriesTheNewStatus() {
        writer.on(new IncidentStatusChanged(
                INCIDENT, "INC-2026-0001", IncidentStatus.REPORTED, IncidentStatus.VERIFIED, USER, Role.DISPATCHER));

        OutboxEvent stored = stored();

        assertThat(stored.getRoutingKey()).isEqualTo("incident.status-changed");
        assertThat(payloadOf(stored).incidentStatus()).isEqualTo("VERIFIED");
    }

    @Test
    void anAssignmentCarriesTheResourceAndItsIncident() {
        writer.on(new ResourceAssigned(
                UUID.randomUUID(), RESOURCE, "AMBULANCE-17", INCIDENT, USER, Role.DISPATCHER));

        OutboxEvent stored = stored();
        IntegrationEvent event = payloadOf(stored);

        assertThat(stored.getRoutingKey()).isEqualTo("resource.assigned");
        assertThat(event.resourceId()).isEqualTo(RESOURCE);
        assertThat(event.callSign()).isEqualTo("AMBULANCE-17");
        assertThat(event.incidentId()).isEqualTo(INCIDENT);
    }

    @Test
    void aReleaseIsStoredForPublishing() {
        writer.on(new ResourceReleased(
                UUID.randomUUID(), RESOURCE, "AMBULANCE-17", INCIDENT, ResourceStatus.EN_ROUTE, USER,
                Role.DISPATCHER));

        assertThat(stored().getRoutingKey()).isEqualTo("resource.released");
    }

    private OutboxEvent stored() {
        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(saved.capture());
        return saved.getValue();
    }

    private IntegrationEvent payloadOf(OutboxEvent stored) {
        return objectMapper.readValue(stored.getPayload(), IntegrationEvent.class);
    }
}
