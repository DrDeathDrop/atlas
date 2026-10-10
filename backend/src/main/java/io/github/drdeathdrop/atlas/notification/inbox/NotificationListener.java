package io.github.drdeathdrop.atlas.notification.inbox;

import io.github.drdeathdrop.atlas.incident.IncidentReported;
import io.github.drdeathdrop.atlas.incident.IncidentStatusChanged;
import io.github.drdeathdrop.atlas.resource.ResourceAssigned;
import io.github.drdeathdrop.atlas.resource.ResourceReleased;
import io.github.drdeathdrop.atlas.user.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;
import java.util.UUID;

@Component
public class NotificationListener {
    static final Set<Role> COORDINATORS = Set.of(Role.ADMIN, Role.DISPATCHER);
    static final Set<Role> FIELD = Set.of(Role.FIELD_OPERATOR);

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationService notifications;

    public NotificationListener(NotificationService notifications) {
        this.notifications = notifications;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(IncidentReported event) {
        send(COORDINATORS, event.reportedBy(), NotificationType.INCIDENT_REPORTED,
                event.reference() + " was reported", event.incidentId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(IncidentStatusChanged event) {
        send(COORDINATORS, event.changedBy(), NotificationType.INCIDENT_STATUS_CHANGED,
                event.reference() + " is now " + event.to().name().toLowerCase(), event.incidentId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ResourceAssigned event) {
        send(FIELD, event.assignedBy(), NotificationType.RESOURCE_ASSIGNED,
                event.callSign() + " was assigned to an incident", event.incidentId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ResourceReleased event) {
        send(FIELD, event.releasedBy(), NotificationType.RESOURCE_RELEASED,
                event.callSign() + " was released", event.incidentId());
    }

    private void send(Set<Role> roles, UUID exceptUser, NotificationType type, String title, UUID incidentId) {
        try {
            notifications.notifyRoles(roles, exceptUser, type, title, incidentId);
        } catch (RuntimeException exception) {
            log.warn("Could not store {} notifications for incident {}", type, incidentId, exception);
        }
    }
}
