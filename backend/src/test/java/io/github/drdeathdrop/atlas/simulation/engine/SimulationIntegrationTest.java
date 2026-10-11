package io.github.drdeathdrop.atlas.simulation.engine;

import io.github.drdeathdrop.atlas.simulation.Scenario;
import io.github.drdeathdrop.atlas.simulation.SimulationStatus;
import io.github.drdeathdrop.atlas.simulation.SimulationSummary;
import io.github.drdeathdrop.atlas.user.Role;
import io.github.drdeathdrop.atlas.user.account.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import io.github.drdeathdrop.atlas.support.IntegrationTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class SimulationIntegrationTest {

    private static final String EMAIL_SUFFIX = "@it.atlas.test";

    @Autowired
    private SimulationService simulationService;

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID admin;

    @BeforeEach
    void setUp() {
        String email = "admin-" + UUID.randomUUID() + EMAIL_SUFFIX;
        admin = userService.createUser(email, "integration-test-1", "Test Admin", Role.ADMIN).id();
        jdbc.update("update simulation_runs set status = 'STOPPED', finished_at = now() where status = 'RUNNING'");
    }

    @AfterEach
    void removeTestData() {
        String pattern = "%" + EMAIL_SUFFIX;
        jdbc.update("delete from simulation_runs where started_by in (select id from users where email like ?)",
                pattern);
        jdbc.update("delete from incidents where reported_by in (select id from users where email like ?)", pattern);
        jdbc.update("delete from refresh_tokens where user_id in (select id from users where email like ?)", pattern);
        jdbc.update("delete from users where email like ?", pattern);
    }

    @Test
    void aRunReportsItsIncidentsOneByOneAndThenCompletes() {
        SimulationSummary started = simulationService.start(
                new StartSimulationRequest(Scenario.FLOOD, 42.1354, 24.7453, 5.0, 3, 1, false, 11L), admin);

        assertThat(started.status()).isEqualTo(SimulationStatus.RUNNING);

        awaitStatus(started.id(), SimulationStatus.COMPLETED);

        Integer linked = jdbc.queryForObject(
                "select count(*) from simulation_incidents where run_id = ?", Integer.class, started.id());
        Integer reported = jdbc.queryForObject("""
                select count(*)
                from incidents i
                join simulation_incidents s on s.incident_id = i.id
                where s.run_id = ? and i.reported_by = ? and i.status = 'REPORTED' and i.title like 'Simulated %'
                """, Integer.class, started.id(), admin);
        Integer audited = jdbc.queryForObject("""
                select count(*)
                from audit_log a
                join simulation_incidents s on s.incident_id = a.entity_id
                where s.run_id = ? and a.action = 'INCIDENT_REPORTED'
                """, Integer.class, started.id());

        assertThat(linked).isEqualTo(3);
        assertThat(reported).isEqualTo(3);
        assertThat(audited).isEqualTo(3);
    }

    @Test
    void onlyOneRunMayBeRunningAtATime() {
        SimulationSummary first = simulationService.start(
                new StartSimulationRequest(Scenario.WILDFIRE, 42.1354, 24.7453, 5.0, 50, 120, false, 1L), admin);

        assertThatThrownBy(() -> simulationService.start(
                new StartSimulationRequest(Scenario.FLOOD, 42.1354, 24.7453, 5.0, 2, 1, false, 2L), admin))
                .isInstanceOf(SimulationAlreadyRunningException.class);

        SimulationSummary stopped = simulationService.stop(first.id());

        assertThat(stopped.status()).isEqualTo(SimulationStatus.STOPPED);
        assertThat(stopped.created()).isLessThan(50);
    }

    private void awaitStatus(UUID runId, SimulationStatus expected) {
        long deadline = System.currentTimeMillis() + 20_000;
        while (true) {
            String status = jdbc.queryForObject(
                    "select status from simulation_runs where id = ?", String.class, runId);
            if (expected.name().equals(status)) {
                return;
            }
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("The run was still " + status + " after 20 seconds");
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for the run", exception);
            }
        }
    }
}
