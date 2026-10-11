package io.github.drdeathdrop.atlas.simulation.engine;

import io.github.drdeathdrop.atlas.incident.IncidentIntake;
import io.github.drdeathdrop.atlas.incident.IncidentSummary;
import io.github.drdeathdrop.atlas.incident.NewIncident;
import io.github.drdeathdrop.atlas.user.Role;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class SimulationRunner {
    private final SimulationRunRepository repository;
    private final ScenarioGenerator generator;
    private final IncidentIntake incidentIntake;

    public SimulationRunner(SimulationRunRepository repository, ScenarioGenerator generator,
                            IncidentIntake incidentIntake) {
        this.repository = repository;
        this.generator = generator;
        this.incidentIntake = incidentIntake;
    }

    @Scheduled(fixedDelayString = "${atlas.simulation.tick:1s}")
    @Transactional
    public void tick() {
        Instant now = Instant.now();

        for (SimulationRun run : repository.lockDue(now)) {
            NewIncident incident = generator.generate(
                    run.getScenario(),
                    run.getLatitude(),
                    run.getLongitude(),
                    run.getRadiusKm(),
                    run.isRelatedIncidents(),
                    run.getSeed(),
                    run.getCreated());

            IncidentSummary reported = incidentIntake.report(incident, run.getStartedBy(), Role.ADMIN);

            repository.linkIncident(run.getId(), reported.id());
            run.recordIncident(now);
        }
    }
}
