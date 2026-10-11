package io.github.drdeathdrop.atlas.simulation.engine;

import io.github.drdeathdrop.atlas.simulation.SimulationStatus;
import io.github.drdeathdrop.atlas.simulation.SimulationSummary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SimulationService {
    private final SimulationRunRepository repository;

    public SimulationService(SimulationRunRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SimulationSummary start(StartSimulationRequest request, UUID startedBy) {
        if (repository.existsByStatus(SimulationStatus.RUNNING)) {
            throw new SimulationAlreadyRunningException();
        }

        long seed = request.seed() != null ? request.seed() : ThreadLocalRandom.current().nextLong();

        SimulationRun run = new SimulationRun(
                request.scenario(),
                request.latitude(),
                request.longitude(),
                request.radiusKm(),
                request.incidents(),
                request.intervalSeconds(),
                Boolean.TRUE.equals(request.relatedIncidents()),
                seed,
                startedBy,
                Instant.now());

        try {
            return toSummary(repository.saveAndFlush(run));
        } catch (DataIntegrityViolationException exception) {
            throw new SimulationAlreadyRunningException();
        }
    }

    @Transactional
    public SimulationSummary stop(UUID runId) {
        SimulationRun run = repository.findByIdForUpdate(runId)
                .orElseThrow(() -> new SimulationNotFoundException(runId));

        if (run.isRunning()) {
            run.stop(Instant.now());
        }

        return toSummary(run);
    }

    @Transactional
    public SimulationSummary resume(UUID runId) {
        SimulationRun run = repository.findByIdForUpdate(runId)
                .orElseThrow(() -> new SimulationNotFoundException(runId));

        if (!run.isStopped()) {
            throw new SimulationNotResumableException();
        }
        if (repository.existsByStatus(SimulationStatus.RUNNING)) {
            throw new SimulationAlreadyRunningException();
        }

        run.resume(Instant.now());

        try {
            return toSummary(repository.saveAndFlush(run));
        } catch (DataIntegrityViolationException exception) {
            throw new SimulationAlreadyRunningException();
        }
    }

    @Transactional(readOnly = true)
    public List<SimulationSummary> latest() {
        return repository.findTop20ByOrderByStartedAtDesc().stream()
                .map(SimulationService::toSummary)
                .toList();
    }

    static SimulationSummary toSummary(SimulationRun run) {
        return new SimulationSummary(
                run.getId(),
                run.getScenario(),
                run.getLatitude(),
                run.getLongitude(),
                run.getRadiusKm(),
                run.getPlanned(),
                run.getCreated(),
                run.getIntervalSeconds(),
                run.isRelatedIncidents(),
                run.getStatus(),
                run.getStartedAt(),
                run.getFinishedAt());
    }
}
