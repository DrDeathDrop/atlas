package io.github.drdeathdrop.atlas.messaging.outbox;

import io.github.drdeathdrop.atlas.messaging.EventChannels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class OutboxRelay {
    static final int BATCH_SIZE = 50;
    static final Duration KEEP_PUBLISHED_FOR = Duration.ofDays(7);

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private boolean brokerWasDown;

    public OutboxRelay(OutboxEventRepository repository, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelayString = "${atlas.events.relay-delay:1s}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = repository.lockNextUnpublished(BATCH_SIZE);

        for (OutboxEvent event : pending) {
            try {
                rabbitTemplate.send(EventChannels.EXCHANGE, event.getRoutingKey(), toMessage(event));
            } catch (AmqpException exception) {
                if (!brokerWasDown) {
                    log.warn("The message broker is unreachable; events stay in the outbox until it is back: {}",
                            exception.getMessage());
                }
                brokerWasDown = true;
                return;
            }
            event.markPublished(Instant.now());
        }

        if (brokerWasDown && !pending.isEmpty()) {
            log.info("The message broker is reachable again; {} waiting events were published", pending.size());
            brokerWasDown = false;
        }
    }

    @Scheduled(fixedDelayString = "${atlas.events.cleanup-delay:1h}")
    @Transactional
    public void removeOldPublished() {
        int removed = repository.deletePublishedBefore(Instant.now().minus(KEEP_PUBLISHED_FOR));
        if (removed > 0) {
            log.info("Removed {} published events from the outbox", removed);
        }
    }

    private static Message toMessage(OutboxEvent event) {
        return MessageBuilder.withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(event.getId().toString())
                .setType(event.getRoutingKey())
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
    }
}
