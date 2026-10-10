package io.github.drdeathdrop.atlas.notification.inbox;

import io.github.drdeathdrop.atlas.incident.IncidentReported;
import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.incident.IncidentStatusChanged;
import io.github.drdeathdrop.atlas.resource.ResourceAssigned;
import io.github.drdeathdrop.atlas.resource.ResourceReleased;
import io.github.drdeathdrop.atlas.resource.ResourceStatus;
import io.github.drdeathdrop.atlas.user.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    private static final UUID INCIDENT = UUID.randomUUID();
    private static final UUID RESOURCE = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();

    @Mock
    private NotificationService notifications;

    @InjectMocks
    private NotificationListener listener;

    @Test
    void aReportedIncidentNotifiesAdminsAndDispatchers() {
        listener.on(new IncidentReported(INCIDENT, "INC-2026-0001", USER, Role.DISPATCHER));

        verify(notifications).notifyRoles(NotificationListener.COORDINATORS, USER,
                NotificationType.INCIDENT_REPORTED, "INC-2026-0001 was reported", INCIDENT);
    }

    @Test
    void aStatusChangeSaysWhatTheIncidentIsNow() {
        listener.on(new IncidentStatusChanged(
                INCIDENT, "INC-2026-0001", IncidentStatus.REPORTED, IncidentStatus.VERIFIED, USER, Role.DISPATCHER));

        verify(notifications).notifyRoles(NotificationListener.COORDINATORS, USER,
                NotificationType.INCIDENT_STATUS_CHANGED, "INC-2026-0001 is now verified", INCIDENT);
    }

    @Test
    void anAssignmentNotifiesFieldOperators() {
        listener.on(new ResourceAssigned(
                UUID.randomUUID(), RESOURCE, "AMBULANCE-17", INCIDENT, USER, Role.DISPATCHER));

        verify(notifications).notifyRoles(NotificationListener.FIELD, USER,
                NotificationType.RESOURCE_ASSIGNED, "AMBULANCE-17 was assigned to an incident", INCIDENT);
    }

    @Test
    void aReleaseNotifiesFieldOperators() {
        listener.on(new ResourceReleased(
                UUID.randomUUID(), RESOURCE, "AMBULANCE-17", INCIDENT, ResourceStatus.EN_ROUTE, USER,
                Role.DISPATCHER));

        verify(notifications).notifyRoles(NotificationListener.FIELD, USER,
                NotificationType.RESOURCE_RELEASED, "AMBULANCE-17 was released", INCIDENT);
    }

    @Test
    void aFailureToStoreNotificationsDoesNotReachTheCaller() {
        doThrow(new IllegalStateException("database down"))
                .when(notifications).notifyRoles(anyCollection(), any(), any(), anyString(), any());

        assertThatCode(() -> listener.on(new IncidentReported(INCIDENT, "INC-2026-0001", USER, Role.DISPATCHER)))
                .doesNotThrowAnyException();
    }
}
