package io.github.drdeathdrop.atlas.messaging.outbox;

import io.github.drdeathdrop.atlas.messaging.EventChannels;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private final MeterRegistry registry = new SimpleMeterRegistry();

    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        relay = new OutboxRelay(repository, rabbitTemplate, registry);
    }

    @Test
    void pendingEventsArePublishedWithTheirIdAndMarked() {
        OutboxEvent event = event("incident.reported", "{\"type\":\"incident.reported\"}");
        when(repository.lockNextUnpublished(OutboxRelay.BATCH_SIZE)).thenReturn(List.of(event));

        relay.publishPending();

        ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(EventChannels.EXCHANGE), eq("incident.reported"), sent.capture());
        Message message = sent.getValue();
        assertThat(new String(message.getBody(), StandardCharsets.UTF_8)).isEqualTo(event.getPayload());
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo(event.getId().toString());
        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(message.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(registry.get(OutboxRelay.PUBLISHED).counter().count()).isEqualTo(1.0);
    }

    @Test
    void nothingIsSentWhenTheOutboxIsEmpty() {
        when(repository.lockNextUnpublished(OutboxRelay.BATCH_SIZE)).thenReturn(List.of());

        relay.publishPending();

        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
    }

    @Test
    void eventsStayPendingWhileTheBrokerIsDown() {
        OutboxEvent first = event("incident.reported", "{}");
        OutboxEvent second = event("resource.assigned", "{}");
        when(repository.lockNextUnpublished(OutboxRelay.BATCH_SIZE)).thenReturn(List.of(first, second));
        doThrow(new AmqpConnectException(new RuntimeException("connection refused")))
                .when(rabbitTemplate).send(anyString(), anyString(), any(Message.class));

        relay.publishPending();

        assertThat(first.getPublishedAt()).isNull();
        assertThat(second.getPublishedAt()).isNull();
        verify(rabbitTemplate, times(1)).send(anyString(), anyString(), any(Message.class));
    }

    @Test
    void whatWasSentBeforeTheBrokerWentDownStaysMarked() {
        OutboxEvent first = event("incident.reported", "{}");
        OutboxEvent second = event("resource.assigned", "{}");
        when(repository.lockNextUnpublished(OutboxRelay.BATCH_SIZE)).thenReturn(List.of(first, second));
        doNothing()
                .doThrow(new AmqpConnectException(new RuntimeException("connection refused")))
                .when(rabbitTemplate).send(anyString(), anyString(), any(Message.class));

        relay.publishPending();

        assertThat(first.getPublishedAt()).isNotNull();
        assertThat(second.getPublishedAt()).isNull();
    }

    @Test
    void oldPublishedEventsAreRemoved() {
        when(repository.deletePublishedBefore(any(Instant.class))).thenReturn(3);

        relay.removeOldPublished();

        ArgumentCaptor<Instant> before = ArgumentCaptor.forClass(Instant.class);
        verify(repository).deletePublishedBefore(before.capture());
        assertThat(before.getValue()).isBefore(Instant.now().minus(OutboxRelay.KEEP_PUBLISHED_FOR).plusSeconds(5));
    }

    private static OutboxEvent event(String routingKey, String payload) {
        return new OutboxEvent(UUID.randomUUID(), routingKey, payload, Instant.now());
    }
}
