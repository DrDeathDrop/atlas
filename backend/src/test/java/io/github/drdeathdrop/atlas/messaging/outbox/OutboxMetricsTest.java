package io.github.drdeathdrop.atlas.messaging.outbox;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxMetricsTest {

    private final MeterRegistry registry = new SimpleMeterRegistry();

    @Mock
    private OutboxEventRepository repository;

    @BeforeEach
    void setUp() {
        new OutboxMetrics(repository, registry);
    }

    @Test
    void theNumberOfWaitingEventsIsReadWhenAsked() {
        when(repository.countByPublishedAtIsNull()).thenReturn(7L);

        assertThat(registry.get(OutboxMetrics.PENDING).gauge().value()).isEqualTo(7.0);
    }

    @Test
    void theAgeOfTheOldestWaitingEventIsInSeconds() {
        when(repository.findOldestUnpublishedCreatedAt()).thenReturn(Instant.now().minusSeconds(90));

        assertThat(registry.get(OutboxMetrics.OLDEST_PENDING_AGE).gauge().value()).isBetween(90.0, 95.0);
    }

    @Test
    void anEmptyOutboxHasNoAge() {
        when(repository.findOldestUnpublishedCreatedAt()).thenReturn(null);

        assertThat(registry.get(OutboxMetrics.OLDEST_PENDING_AGE).gauge().value()).isZero();
    }
}
