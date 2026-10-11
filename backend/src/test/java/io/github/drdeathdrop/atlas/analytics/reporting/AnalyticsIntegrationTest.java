package io.github.drdeathdrop.atlas.analytics.reporting;

import io.github.drdeathdrop.atlas.analytics.AnalyticsOverview;
import io.github.drdeathdrop.atlas.analytics.AnalyticsOverview.Count;
import io.github.drdeathdrop.atlas.analytics.AnalyticsOverview.DayCount;
import io.github.drdeathdrop.atlas.incident.IncidentCategory;
import io.github.drdeathdrop.atlas.incident.IncidentIntake;
import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.incident.IncidentSummary;
import io.github.drdeathdrop.atlas.incident.NewIncident;
import io.github.drdeathdrop.atlas.incident.Severity;
import io.github.drdeathdrop.atlas.incident.core.IncidentService;
import io.github.drdeathdrop.atlas.user.Role;
import io.github.drdeathdrop.atlas.user.account.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import io.github.drdeathdrop.atlas.support.IntegrationTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class AnalyticsIntegrationTest {

    private static final String EMAIL_SUFFIX = "@it.atlas.test";

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private AnalyticsQueries queries;

    @Autowired
    private IncidentIntake incidentIntake;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID dispatcher;

    @BeforeEach
    void setUp() {
        String email = "dispatcher-" + UUID.randomUUID() + EMAIL_SUFFIX;
        dispatcher = userService.createUser(email, "integration-test-1", "Test Dispatcher", Role.DISPATCHER).id();
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
    void theOverviewCountsWhatWasReportedInThePeriod() {
        AnalyticsOverview before = analyticsService.overview(7, true);

        report(IncidentCategory.AVALANCHE, 42.60, 23.20);
        report(IncidentCategory.AVALANCHE, 42.60, 23.20);
        IncidentSummary verified = report(IncidentCategory.SINKHOLE, 42.10, 24.70);
        incidentService.changeStatus(verified.id(), IncidentStatus.VERIFIED, dispatcher, Role.DISPATCHER);

        AnalyticsOverview after = analyticsService.overview(7, true);

        assertThat(after.days()).isEqualTo(7);
        assertThat(after.totals().reported()).isEqualTo(before.totals().reported() + 3);
        assertThat(after.totals().open()).isEqualTo(before.totals().open() + 3);
        assertThat(countOf(after, "AVALANCHE")).isEqualTo(countOf(before, "AVALANCHE") + 2);

        assertThat(after.perDay()).hasSize(7);
        DayCount today = after.perDay().get(6);
        assertThat(today.day()).isEqualTo(LocalDate.now(ZoneOffset.UTC).toString());
        assertThat(today.count()).isEqualTo(before.perDay().get(6).count() + 3);
        assertThat(after.perDay().stream().mapToLong(DayCount::count).sum()).isEqualTo(after.totals().reported());

        assertThat(after.responseTimes().toVerify().samples())
                .isEqualTo(before.responseTimes().toVerify().samples() + 1);
        assertThat(after.responseTimes().toVerify().medianMinutes()).isNotNull().isGreaterThanOrEqualTo(0.0);

        Instant weekAgo = Instant.now().minus(Duration.ofDays(7));
        assertThat(queries.hotspots(weekAgo, true, AnalyticsService.HOTSPOT_CELL_DEGREES, 100_000)).anySatisfy(hotspot -> {
            assertThat(hotspot.latitude()).isBetween(42.59, 42.61);
            assertThat(hotspot.longitude()).isBetween(23.19, 23.21);
            assertThat(hotspot.incidents()).isGreaterThanOrEqualTo(2);
        });
    }

    @Test
    void simulatedIncidentsAreLeftOutUnlessAskedFor() {
        IncidentSummary real = report(IncidentCategory.FLOOD, 42.13, 24.74);
        IncidentSummary simulated = report(IncidentCategory.FLOOD, 42.14, 24.75);
        UUID run = UUID.randomUUID();
        jdbc.update("""
                insert into simulation_runs (id, scenario, latitude, longitude, radius_km, planned, created,
                    interval_seconds, related_incidents, seed, status, started_by, started_at, next_incident_at,
                    finished_at)
                values (?, 'FLOOD', 42.1, 24.7, 5, 1, 1, 5, false, 1, 'COMPLETED', ?, now(), now(), now())
                """, run, dispatcher);
        jdbc.update("insert into simulation_incidents (run_id, incident_id) values (?, ?)", run, simulated.id());

        AnalyticsOverview withSimulated = analyticsService.overview(7, true);
        AnalyticsOverview withoutSimulated = analyticsService.overview(7, false);

        assertThat(real.id()).isNotEqualTo(simulated.id());
        assertThat(withSimulated.totals().simulated()).isGreaterThanOrEqualTo(1);
        assertThat(withoutSimulated.totals().simulated()).isEqualTo(withSimulated.totals().simulated());
        assertThat(withSimulated.totals().reported() - withoutSimulated.totals().reported())
                .isEqualTo(withSimulated.totals().simulated());
        assertThat(countOf(withSimulated, "FLOOD") - countOf(withoutSimulated, "FLOOD"))
                .isGreaterThanOrEqualTo(1);
    }

    @Test
    void resourceFiguresAreReported() {
        AnalyticsOverview overview = analyticsService.overview(30, false);

        assertThat(overview.resourcesByStatus()).allSatisfy(count -> assertThat(count.count()).isPositive());
        assertThat(overview.busiestResources()).hasSizeLessThanOrEqualTo(AnalyticsService.BUSIEST_RESOURCES);
        assertThat(overview.bySeverity()).isNotNull();
        assertThat(overview.byStatus()).isNotNull();
    }

    private IncidentSummary report(IncidentCategory category, double latitude, double longitude) {
        return incidentIntake.report(new NewIncident(
                "Analytics test incident", "Created by a test.", category, Severity.LOW, latitude, longitude, 0),
                dispatcher, Role.DISPATCHER);
    }

    private static long countOf(AnalyticsOverview overview, String category) {
        return overview.byCategory().stream()
                .filter(count -> count.name().equals(category))
                .mapToLong(Count::count)
                .findFirst()
                .orElse(0);
    }
}
