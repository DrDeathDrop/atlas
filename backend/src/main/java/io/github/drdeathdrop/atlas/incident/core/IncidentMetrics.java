package io.github.drdeathdrop.atlas.incident.core;

import io.github.drdeathdrop.atlas.incident.IncidentReported;
import io.github.drdeathdrop.atlas.incident.IncidentStatusChanged;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class IncidentMetrics {
    static final String REPORTED = "atlas.incidents.reported";
    static final String STATUS_CHANGES = "atlas.incidents.status.changes";

    private final MeterRegistry registry;
    private final Counter reported;

    public IncidentMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.reported = Counter.builder(REPORTED)
                .description("Incidents reported since the application started")
                .register(registry);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(IncidentReported event) {
        reported.increment();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(IncidentStatusChanged event) {
        Counter.builder(STATUS_CHANGES)
                .description("Incident status changes, by the status moved to")
                .tag("to", event.to().name())
                .register(registry)
                .increment();
    }
}
