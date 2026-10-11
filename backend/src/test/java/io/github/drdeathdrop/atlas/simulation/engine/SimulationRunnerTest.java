package io.github.drdeathdrop.atlas.simulation.engine;

import io.github.drdeathdrop.atlas.incident.IncidentCategory;
import io.github.drdeathdrop.atlas.incident.IncidentIntake;
import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.incident.IncidentSummary;
import io.github.drdeathdrop.atlas.incident.NewIncident;
import io.github.drdeathdrop.atlas.incident.Severity;
import io.github.drdeathdrop.atlas.simulation.Scenario;
import io.github.drdeathdrop.atlas.simulation.SimulationStatus;
import io.github.drdeathdrop.atlas.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimulationRunnerTest {

    private static final UUID ADMIN = UUID.randomUUID();

    @Mock
    private SimulationRunRepository repository;

    @Mock
    private IncidentIntake incidentIntake;

    private SimulationRunner runner;

    @BeforeEach
    void setUp() {
        runner = new SimulationRunner(repository, new ScenarioGenerator(), incidentIntake);
    }

    @Test
    void aDueRunReportsOneIncidentThroughTheNormalIntake() {
        SimulationRun run = run(3);
        UUID incidentId = UUID.randomUUID();
        when(repository.lockDue(any(Instant.class))).thenReturn(List.of(run));
        when(incidentIntake.report(any(NewIncident.class), eq(ADMIN), eq(Role.ADMIN)))
                .thenReturn(summary(incidentId));

        runner.tick();

        ArgumentCaptor<NewIncident> reported = ArgumentCaptor.forClass(NewIncident.class);
        verify(incidentIntake).report(reported.capture(), eq(ADMIN), eq(Role.ADMIN));
        assertThat(reported.getValue().title()).endsWith("#1");
        verify(repository).linkIncident(run.getId(), incidentId);
        assertThat(run.getCreated()).isEqualTo(1);
        assertThat(run.isRunning()).isTrue();
    }

    @Test
    void theLastIncidentCompletesTheRun() {
        SimulationRun run = run(1);
        when(repository.lockDue(any(Instant.class))).thenReturn(List.of(run));
        when(incidentIntake.report(any(NewIncident.class), eq(ADMIN), eq(Role.ADMIN)))
                .thenReturn(summary(UUID.randomUUID()));

        runner.tick();

        assertThat(run.getStatus()).isEqualTo(SimulationStatus.COMPLETED);
    }

    @Test
    void nothingHappensWhenNoRunIsDue() {
        when(repository.lockDue(any(Instant.class))).thenReturn(List.of());

        runner.tick();

        verifyNoInteractions(incidentIntake);
    }

    private static SimulationRun run(int planned) {
        SimulationRun run = new SimulationRun(Scenario.FLOOD, 42.1354, 24.7453, 5, planned, 5, false, 1L, ADMIN,
                Instant.now());
        ReflectionTestUtils.setField(run, "id", UUID.randomUUID());
        return run;
    }

    private static IncidentSummary summary(UUID id) {
        return new IncidentSummary(id, "INC-2026-0001", "Simulated flood #1", "x", IncidentCategory.FLOOD,
                Severity.LOW, IncidentStatus.REPORTED, 42.1, 24.7, 0, ADMIN, Instant.now(), Instant.now());
    }
}
