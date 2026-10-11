package io.github.drdeathdrop.atlas.notification.inbox;

import io.github.drdeathdrop.atlas.messaging.EventChannels;
import io.github.drdeathdrop.atlas.messaging.IntegrationEvent;
import io.github.drdeathdrop.atlas.user.Role;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

@Component
public class NotificationConsumer {
    static final Set<Role> COORDINATORS = Set.of(Role.ADMIN, Role.DISPATCHER);
    static final Set<Role> FIELD = Set.of(Role.FIELD_OPERATOR);
    static final String EVENTS = "atlas.notifications.events";
    static final String OUTCOME = "outcome";

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationService notifications;
    private final ObjectMapper objectMapper;
    private final Counter handled;
    private final Counter duplicates;
    private final Counter rejected;

    public NotificationConsumer(NotificationService notifications, ObjectMapper objectMapper,
                                MeterRegistry registry) {
        this.notifications = notifications;
        this.objectMapper = objectMapper;
        this.handled = outcome(registry, "handled");
        this.duplicates = outcome(registry, "duplicate");
        this.rejected = outcome(registry, "rejected");
    }

    @RabbitListener(queues = EventChannels.NOTIFICATIONS_QUEUE)
    public void receive(Message message) {
        IntegrationEvent event;
        try {
            event = read(message);
        } catch (AmqpRejectAndDontRequeueException exception) {
            rejected.increment();
            throw exception;
        }

        try {
            handle(event);
            handled.increment();
        } catch (DataIntegrityViolationException exception) {
            duplicates.increment();
            log.info("Skipped event {}: it was already handled or its incident no longer exists", event.eventId());
        } catch (RuntimeException exception) {
            rejected.increment();
            throw new AmqpRejectAndDontRequeueException("Could not handle event " + event.eventId(), exception);
        }
    }

    private static Counter outcome(MeterRegistry registry, String outcome) {
        return Counter.builder(EVENTS)
                .description("Events received from the broker, by what happened to them")
                .tag(OUTCOME, outcome)
                .register(registry);
    }

    void handle(IntegrationEvent event) {
        switch (event.type()) {
            case IntegrationEvent.INCIDENT_REPORTED -> notifications.notifyRoles(
                    event.eventId(), COORDINATORS, event.actorId(), NotificationType.INCIDENT_REPORTED,
                    event.incidentReference() + " was reported", event.incidentId());
            case IntegrationEvent.INCIDENT_STATUS_CHANGED -> notifications.notifyRoles(
                    event.eventId(), COORDINATORS, event.actorId(), NotificationType.INCIDENT_STATUS_CHANGED,
                    event.incidentReference() + " is now " + event.incidentStatus().toLowerCase(),
                    event.incidentId());
            case IntegrationEvent.RESOURCE_ASSIGNED -> notifications.notifyRoles(
                    event.eventId(), FIELD, event.actorId(), NotificationType.RESOURCE_ASSIGNED,
                    event.callSign() + " was assigned to an incident", event.incidentId());
            case IntegrationEvent.RESOURCE_RELEASED -> notifications.notifyRoles(
                    event.eventId(), FIELD, event.actorId(), NotificationType.RESOURCE_RELEASED,
                    event.callSign() + " was released", event.incidentId());
            default -> log.debug("Ignored an event of type {}", event.type());
        }
    }

    private IntegrationEvent read(Message message) {
        try {
            IntegrationEvent event = objectMapper.readValue(message.getBody(), IntegrationEvent.class);
            if (event == null || event.eventId() == null || event.type() == null) {
                throw new AmqpRejectAndDontRequeueException("The event has no id or type");
            }
            return event;
        } catch (JacksonException exception) {
            throw new AmqpRejectAndDontRequeueException("The event could not be read", exception);
        }
    }
}
