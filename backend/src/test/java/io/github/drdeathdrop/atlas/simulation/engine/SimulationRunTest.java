package io.github.drdeathdrop.atlas.simulation.engine;

import io.github.drdeathdrop.atlas.simulation.Scenario;
import io.github.drdeathdrop.atlas.simulation.SimulationStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SimulationRunTest {

    private static final Instant START = Instant.parse("2026-10-11T12:00:00Z");

    @Test
    void aNewRunIsDueImmediately() {
        SimulationRun run = run(3, 10);

        assertThat(run.isRunning()).isTrue();
        assertThat(run.getCreated()).isZero();
        assertThat(run.getNextIncidentAt()).isEqualTo(START);
    }

    @Test
    void eachIncidentSchedulesTheNextOneAnIntervalLater() {
        SimulationRun run = run(3, 10);

        run.recordIncident(START);

        assertThat(run.getCreated()).isEqualTo(1);
        assertThat(run.getNextIncidentAt()).isEqualTo(START.plusSeconds(10));
        assertThat(run.isRunning()).isTrue();
    }

    @Test
    void theLastIncidentCompletesTheRun() {
        SimulationRun run = run(2, 10);

        run.recordIncident(START);
        run.recordIncident(START.plusSeconds(10));

        assertThat(run.getStatus()).isEqualTo(SimulationStatus.COMPLETED);
        assertThat(run.getFinishedAt()).isEqualTo(START.plusSeconds(10));
    }

    @Test
    void aRunCanBeStoppedEarly() {
        SimulationRun run = run(5, 10);
        run.recordIncident(START);

        run.stop(START.plusSeconds(3));

        assertThat(run.getStatus()).isEqualTo(SimulationStatus.STOPPED);
        assertThat(run.isRunning()).isFalse();
        assertThat(run.getCreated()).isEqualTo(1);
    }

    private static SimulationRun run(int planned, int intervalSeconds) {
        return new SimulationRun(Scenario.FLOOD, 42.1354, 24.7453, 5, planned, intervalSeconds, false, 1L,
                UUID.randomUUID(), START);
    }
}
