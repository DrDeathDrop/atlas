package io.github.drdeathdrop.atlas.notification.inbox;

import io.github.drdeathdrop.atlas.messaging.IntegrationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    private static final UUID EVENT = UUID.randomUUID();
    private static final UUID INCIDENT = UUID.randomUUID();
    private static final UUID RESOURCE = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Mock
    private NotificationService notifications;

    private NotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new NotificationConsumer(notifications, objectMapper);
    }

    @Test
    void aReportedIncidentNotifiesAdminsAndDispatchers() {
        consumer.receive(message(new IntegrationEvent(EVENT, IntegrationEvent.INCIDENT_REPORTED,
                INCIDENT, "INC-2026-0001", null, null, null, ACTOR)));

        verify(notifications).notifyRoles(EVENT, NotificationConsumer.COORDINATORS, ACTOR,
                NotificationType.INCIDENT_REPORTED, "INC-2026-0001 was reported", INCIDENT);
    }

    @Test
    void aStatusChangeSaysWhatTheIncidentIsNow() {
        consumer.receive(message(new IntegrationEvent(EVENT, IntegrationEvent.INCIDENT_STATUS_CHANGED,
                INCIDENT, "INC-2026-0001", "VERIFIED", null, null, ACTOR)));

        verify(notifications).notifyRoles(EVENT, NotificationConsumer.COORDINATORS, ACTOR,
                NotificationType.INCIDENT_STATUS_CHANGED, "INC-2026-0001 is now verified", INCIDENT);
    }

    @Test
    void anAssignmentNotifiesFieldOperators() {
        consumer.receive(message(new IntegrationEvent(EVENT, IntegrationEvent.RESOURCE_ASSIGNED,
                INCIDENT, null, null, RESOURCE, "AMBULANCE-17", ACTOR)));

        verify(notifications).notifyRoles(EVENT, NotificationConsumer.FIELD, ACTOR,
                NotificationType.RESOURCE_ASSIGNED, "AMBULANCE-17 was assigned to an incident", INCIDENT);
    }

    @Test
    void aReleaseNotifiesFieldOperators() {
        consumer.receive(message(new IntegrationEvent(EVENT, IntegrationEvent.RESOURCE_RELEASED,
                INCIDENT, null, null, RESOURCE, "AMBULANCE-17", ACTOR)));

        verify(notifications).notifyRoles(EVENT, NotificationConsumer.FIELD, ACTOR,
                NotificationType.RESOURCE_RELEASED, "AMBULANCE-17 was released", INCIDENT);
    }

    @Test
    void anEventOfAnUnknownTypeIsAcceptedAndIgnored() {
        consumer.receive(message(new IntegrationEvent(EVENT, "zone.changed",
                null, null, null, null, null, ACTOR)));

        verifyNoInteractions(notifications);
    }

    @Test
    void aMessageThatCannotBeReadGoesToTheDeadLetterQueue() {
        Message unreadable = MessageBuilder.withBody("not json".getBytes(StandardCharsets.UTF_8)).build();

        assertThatThrownBy(() -> consumer.receive(unreadable))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(notifications);
    }

    @Test
    void aMessageWithoutAnIdGoesToTheDeadLetterQueue() {
        Message incomplete = MessageBuilder.withBody("{\"type\":\"incident.reported\"}"
                .getBytes(StandardCharsets.UTF_8)).build();

        assertThatThrownBy(() -> consumer.receive(incomplete))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void aFailureWhileHandlingSendsTheMessageToTheDeadLetterQueue() {
        doThrow(new IllegalStateException("database down"))
                .when(notifications).notifyRoles(any(), anyCollection(), any(), any(), anyString(), any());

        assertThatThrownBy(() -> consumer.receive(message(new IntegrationEvent(EVENT,
                IntegrationEvent.INCIDENT_REPORTED, INCIDENT, "INC-2026-0001", null, null, null, ACTOR))))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void aDuplicateOrAnEventForADeletedIncidentIsDroppedQuietly() {
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(notifications).notifyRoles(any(), anyCollection(), any(), any(), anyString(), any());

        assertThatCode(() -> consumer.receive(message(new IntegrationEvent(EVENT,
                IntegrationEvent.INCIDENT_REPORTED, INCIDENT, "INC-2026-0001", null, null, null, ACTOR))))
                .doesNotThrowAnyException();
    }

    private Message message(IntegrationEvent event) {
        return MessageBuilder.withBody(objectMapper.writeValueAsBytes(event)).build();
    }
}
