package io.github.drdeathdrop.atlas.notification.live;

import io.github.drdeathdrop.atlas.facility.FacilityChanged;
import io.github.drdeathdrop.atlas.incident.IncidentReported;
import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.incident.IncidentStatusChanged;
import io.github.drdeathdrop.atlas.notification.NotificationsCreated;
import io.github.drdeathdrop.atlas.resource.ResourceAssigned;
import io.github.drdeathdrop.atlas.resource.ResourceChanged;
import io.github.drdeathdrop.atlas.resource.ResourceReleased;
import io.github.drdeathdrop.atlas.resource.ResourceStatus;
import io.github.drdeathdrop.atlas.user.Role;
import io.github.drdeathdrop.atlas.zone.RoadClosureChanged;
import io.github.drdeathdrop.atlas.zone.ZoneChanged;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class LiveUpdateBroadcasterTest {

    private static final UUID INCIDENT = UUID.randomUUID();
    private static final UUID RESOURCE = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();

    @Mock
    private SimpMessagingTemplate messaging;

    @InjectMocks
    private LiveUpdateBroadcaster broadcaster;

    @Test
    void aReportedIncidentIsAnnounced() {
        broadcaster.on(new IncidentReported(INCIDENT, "INC-2026-0001", USER, Role.DISPATCHER));

        verifySent(LiveUpdateKind.INCIDENT, INCIDENT);
        verifyNoMoreInteractions(messaging);
    }

    @Test
    void aStatusChangeIsAnnounced() {
        broadcaster.on(new IncidentStatusChanged(
                INCIDENT, "INC-2026-0001", IncidentStatus.REPORTED, IncidentStatus.VERIFIED, USER, Role.DISPATCHER));

        verifySent(LiveUpdateKind.INCIDENT, INCIDENT);
    }

    @Test
    void anAssignmentAnnouncesBothTheResourceAndTheIncident() {
        broadcaster.on(new ResourceAssigned(
                UUID.randomUUID(), RESOURCE, "AMBULANCE-17", INCIDENT, USER, Role.DISPATCHER));

        verifySent(LiveUpdateKind.RESOURCE, RESOURCE);
        verifySent(LiveUpdateKind.INCIDENT, INCIDENT);
    }

    @Test
    void aReleaseAnnouncesBothTheResourceAndTheIncident() {
        broadcaster.on(new ResourceReleased(
                UUID.randomUUID(), RESOURCE, "AMBULANCE-17", INCIDENT, ResourceStatus.EN_ROUTE, USER,
                Role.DISPATCHER));

        verifySent(LiveUpdateKind.RESOURCE, RESOURCE);
        verifySent(LiveUpdateKind.INCIDENT, INCIDENT);
    }

    @Test
    void changesToResourcesFacilitiesAndZonesAreAnnounced() {
        UUID facility = UUID.randomUUID();
        UUID zone = UUID.randomUUID();
        UUID closure = UUID.randomUUID();

        broadcaster.on(new ResourceChanged(RESOURCE));
        broadcaster.on(new FacilityChanged(facility));
        broadcaster.on(new ZoneChanged(zone));
        broadcaster.on(new RoadClosureChanged(closure));

        verifySent(LiveUpdateKind.RESOURCE, RESOURCE);
        verifySent(LiveUpdateKind.FACILITY, facility);
        verifySent(LiveUpdateKind.ZONE, zone);
        verifySent(LiveUpdateKind.ZONE, closure);
    }

    @Test
    void newNotificationsAreAnnouncedWithoutSayingWhoTheyAreFor() {
        broadcaster.on(new NotificationsCreated(3));

        verifySent(LiveUpdateKind.NOTIFICATION, null);
    }

    @Test
    void aBroadcastFailureDoesNotReachTheCaller() {
        doThrow(new MessageDeliveryException("broker down"))
                .when(messaging).convertAndSend(anyString(), any(Object.class));

        assertThatCode(() -> broadcaster.on(new ResourceChanged(RESOURCE))).doesNotThrowAnyException();
    }

    private void verifySent(LiveUpdateKind kind, UUID id) {
        verify(messaging).convertAndSend(LiveUpdateBroadcaster.TOPIC, (Object) new LiveUpdate(kind, id));
    }
}
