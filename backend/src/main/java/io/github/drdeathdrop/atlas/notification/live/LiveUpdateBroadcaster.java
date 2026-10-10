package io.github.drdeathdrop.atlas.notification.live;

import io.github.drdeathdrop.atlas.facility.FacilityChanged;
import io.github.drdeathdrop.atlas.incident.IncidentReported;
import io.github.drdeathdrop.atlas.incident.IncidentStatusChanged;
import io.github.drdeathdrop.atlas.resource.ResourceAssigned;
import io.github.drdeathdrop.atlas.resource.ResourceChanged;
import io.github.drdeathdrop.atlas.resource.ResourceReleased;
import io.github.drdeathdrop.atlas.zone.RoadClosureChanged;
import io.github.drdeathdrop.atlas.zone.ZoneChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
public class LiveUpdateBroadcaster {
    public static final String TOPIC = "/topic/updates";

    private static final Logger log = LoggerFactory.getLogger(LiveUpdateBroadcaster.class);

    private final SimpMessagingTemplate messaging;

    public LiveUpdateBroadcaster(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(IncidentReported event) {
        send(LiveUpdateKind.INCIDENT, event.incidentId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(IncidentStatusChanged event) {
        send(LiveUpdateKind.INCIDENT, event.incidentId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ResourceAssigned event) {
        send(LiveUpdateKind.RESOURCE, event.resourceId());
        send(LiveUpdateKind.INCIDENT, event.incidentId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ResourceReleased event) {
        send(LiveUpdateKind.RESOURCE, event.resourceId());
        send(LiveUpdateKind.INCIDENT, event.incidentId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ResourceChanged event) {
        send(LiveUpdateKind.RESOURCE, event.resourceId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(FacilityChanged event) {
        send(LiveUpdateKind.FACILITY, event.facilityId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ZoneChanged event) {
        send(LiveUpdateKind.ZONE, event.zoneId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(RoadClosureChanged event) {
        send(LiveUpdateKind.ZONE, event.closureId());
    }

    private void send(LiveUpdateKind kind, UUID id) {
        try {
            messaging.convertAndSend(TOPIC, new LiveUpdate(kind, id));
        } catch (MessagingException exception) {
            log.warn("Could not broadcast a {} update for {}", kind, id, exception);
        }
    }
}
