package io.github.drdeathdrop.atlas.simulation.engine;

import io.github.drdeathdrop.atlas.simulation.Scenario;
import io.github.drdeathdrop.atlas.simulation.SimulationStatus;
import io.github.drdeathdrop.atlas.simulation.SimulationSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimulationServiceTest {

    private static final UUID ADMIN = UUID.randomUUID();

    @Mock
    private SimulationRunRepository repository;

    @InjectMocks
    private SimulationService service;

    @Test
    void startCreatesARunningRunWithTheGivenSeed() {
        when(repository.existsByStatus(SimulationStatus.RUNNING)).thenReturn(false);
        when(repository.saveAndFlush(any(SimulationRun.class))).thenAnswer(call -> call.getArgument(0));

        SimulationSummary result = service.start(request(7L), ADMIN);

        assertThat(result.scenario()).isEqualTo(Scenario.FLOOD);
        assertThat(result.status()).isEqualTo(SimulationStatus.RUNNING);
        assertThat(result.planned()).isEqualTo(10);
        assertThat(result.created()).isZero();
        assertThat(result.intervalSeconds()).isEqualTo(5);

        ArgumentCaptor<SimulationRun> saved = ArgumentCaptor.forClass(SimulationRun.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getSeed()).isEqualTo(7L);
        assertThat(saved.getValue().getStartedBy()).isEqualTo(ADMIN);
    }

    @Test
    void startIsRefusedWhileAnotherRunIsRunning() {
        when(repository.existsByStatus(SimulationStatus.RUNNING)).thenReturn(true);

        assertThatThrownBy(() -> service.start(request(null), ADMIN))
                .isInstanceOf(SimulationAlreadyRunningException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void startIsRefusedWhenTheDatabaseSeesASecondRunningRun() {
        when(repository.existsByStatus(SimulationStatus.RUNNING)).thenReturn(false);
        when(repository.saveAndFlush(any(SimulationRun.class)))
                .thenThrow(new DataIntegrityViolationException("uq_simulation_runs_one_running"));

        assertThatThrownBy(() -> service.start(request(null), ADMIN))
                .isInstanceOf(SimulationAlreadyRunningException.class);
    }

    @Test
    void stopEndsARunningRun() {
        UUID id = UUID.randomUUID();
        SimulationRun run = run();
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(run));

        SimulationSummary result = service.stop(id);

        assertThat(result.status()).isEqualTo(SimulationStatus.STOPPED);
        assertThat(result.finishedAt()).isNotNull();
    }

    @Test
    void stoppingAFinishedRunChangesNothing() {
        UUID id = UUID.randomUUID();
        SimulationRun run = run();
        Instant finished = Instant.parse("2026-10-11T12:00:00Z");
        run.recordIncident(finished);
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(run));

        SimulationSummary result = service.stop(id);

        assertThat(result.status()).isEqualTo(SimulationStatus.COMPLETED);
        assertThat(result.finishedAt()).isEqualTo(finished);
    }

    @Test
    void stopFailsForAnUnknownRun() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.stop(id)).isInstanceOf(SimulationNotFoundException.class);
    }

    @Test
    void aStoppedRunCanBeResumedWhereItLeftOff() {
        UUID id = UUID.randomUUID();
        SimulationRun run = new SimulationRun(Scenario.FLOOD, 42.1354, 24.7453, 5, 5, 5, false, 1L, ADMIN,
                Instant.now());
        run.recordIncident(Instant.now());
        run.stop(Instant.now());
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(run));
        when(repository.existsByStatus(SimulationStatus.RUNNING)).thenReturn(false);
        when(repository.saveAndFlush(run)).thenReturn(run);

        SimulationSummary result = service.resume(id);

        assertThat(result.status()).isEqualTo(SimulationStatus.RUNNING);
        assertThat(result.created()).isEqualTo(1);
        assertThat(result.finishedAt()).isNull();
    }

    @Test
    void aCompletedRunCannotBeResumed() {
        UUID id = UUID.randomUUID();
        SimulationRun run = run();
        run.recordIncident(Instant.now());
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> service.resume(id)).isInstanceOf(SimulationNotResumableException.class);
    }

    @Test
    void aStoppedRunCannotBeResumedWhileAnotherIsRunning() {
        UUID id = UUID.randomUUID();
        SimulationRun run = new SimulationRun(Scenario.FLOOD, 42.1354, 24.7453, 5, 5, 5, false, 1L, ADMIN,
                Instant.now());
        run.stop(Instant.now());
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(run));
        when(repository.existsByStatus(SimulationStatus.RUNNING)).thenReturn(true);

        assertThatThrownBy(() -> service.resume(id)).isInstanceOf(SimulationAlreadyRunningException.class);
        assertThat(run.isStopped()).isTrue();
    }

    private static StartSimulationRequest request(Long seed) {
        return new StartSimulationRequest(Scenario.FLOOD, 42.1354, 24.7453, 5.0, 10, 5, false, seed);
    }

    private static SimulationRun run() {
        return new SimulationRun(Scenario.FLOOD, 42.1354, 24.7453, 5, 1, 5, false, 1L, ADMIN, Instant.now());
    }
}
