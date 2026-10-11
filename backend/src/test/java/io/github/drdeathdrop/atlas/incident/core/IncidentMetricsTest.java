package io.github.drdeathdrop.atlas.incident.core;

import io.github.drdeathdrop.atlas.incident.IncidentReported;
import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.incident.IncidentStatusChanged;
import io.github.drdeathdrop.atlas.user.Role;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentMetricsTest {

    private final MeterRegistry registry = new SimpleMeterRegistry();
    private final IncidentMetrics metrics = new IncidentMetrics(registry);

    @Test
    void everyReportedIncidentIsCounted() {
        metrics.on(new IncidentReported(UUID.randomUUID(), "INC-2026-0001", UUID.randomUUID(), Role.DISPATCHER));
        metrics.on(new IncidentReported(UUID.randomUUID(), "INC-2026-0002", UUID.randomUUID(), Role.DISPATCHER));

        assertThat(registry.get(IncidentMetrics.REPORTED).counter().count()).isEqualTo(2.0);
    }

    @Test
    void statusChangesAreCountedByTheStatusMovedTo() {
        metrics.on(change(IncidentStatus.VERIFIED));
        metrics.on(change(IncidentStatus.VERIFIED));
        metrics.on(change(IncidentStatus.RESOLVED));

        assertThat(registry.get(IncidentMetrics.STATUS_CHANGES).tag("to", "VERIFIED").counter().count())
                .isEqualTo(2.0);
        assertThat(registry.get(IncidentMetrics.STATUS_CHANGES).tag("to", "RESOLVED").counter().count())
                .isEqualTo(1.0);
    }

    private static IncidentStatusChanged change(IncidentStatus to) {
        return new IncidentStatusChanged(UUID.randomUUID(), "INC-2026-0001", IncidentStatus.REPORTED, to,
                UUID.randomUUID(), Role.DISPATCHER);
    }
}
