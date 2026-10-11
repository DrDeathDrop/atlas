package io.github.drdeathdrop.atlas.messaging.outbox;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class OutboxMetrics {
    static final String PENDING = "atlas.outbox.pending";
    static final String OLDEST_PENDING_AGE = "atlas.outbox.oldest.pending.age";

    public OutboxMetrics(OutboxEventRepository repository, MeterRegistry registry) {
        Gauge.builder(PENDING, repository, OutboxEventRepository::countByPublishedAtIsNull)
                .description("Events written to the outbox and not yet published to the broker")
                .register(registry);
        Gauge.builder(OLDEST_PENDING_AGE, repository, OutboxMetrics::oldestPendingAgeSeconds)
                .description("How long the oldest unpublished event has been waiting")
                .baseUnit("seconds")
                .register(registry);
    }

    static double oldestPendingAgeSeconds(OutboxEventRepository repository) {
        Instant oldest = repository.findOldestUnpublishedCreatedAt();
        if (oldest == null) {
            return 0;
        }
        return Duration.between(oldest, Instant.now()).toMillis() / 1000.0;
    }
}
