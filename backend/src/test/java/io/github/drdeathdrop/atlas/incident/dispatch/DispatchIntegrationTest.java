package io.github.drdeathdrop.atlas.incident.dispatch;

import io.github.drdeathdrop.atlas.audit.log.AuditAction;
import io.github.drdeathdrop.atlas.audit.log.AuditEntry;
import io.github.drdeathdrop.atlas.audit.log.AuditEntryRepository;
import io.github.drdeathdrop.atlas.incident.IncidentCategory;
import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.incident.IncidentSummary;
import io.github.drdeathdrop.atlas.incident.Severity;
import io.github.drdeathdrop.atlas.incident.core.IncidentService;
import io.github.drdeathdrop.atlas.incident.core.ReportIncidentRequest;
import io.github.drdeathdrop.atlas.resource.AssignmentSummary;
import io.github.drdeathdrop.atlas.resource.ResourceAllocation;
import io.github.drdeathdrop.atlas.resource.ResourceStatus;
import io.github.drdeathdrop.atlas.resource.ResourceSummary;
import io.github.drdeathdrop.atlas.resource.ResourceType;
import io.github.drdeathdrop.atlas.resource.allocation.ResourceNotAvailableException;
import io.github.drdeathdrop.atlas.resource.inventory.CreateResourceRequest;
import io.github.drdeathdrop.atlas.resource.inventory.ResourceService;
import io.github.drdeathdrop.atlas.user.Role;
import io.github.drdeathdrop.atlas.user.account.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class DispatchIntegrationTest {

    private static final String EMAIL_SUFFIX = "@it.atlas.test";
    private static final double LATITUDE = 42.1354;
    private static final double LONGITUDE = 24.7453;

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private ResourceAllocation resourceAllocation;

    @Autowired
    private UserService userService;

    @Autowired
    private AuditEntryRepository auditEntries;

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
        jdbc.update("""
                delete from assignments where incident_id in (
                    select i.id from incidents i join users u on u.id = i.reported_by where u.email like ?)
                """, pattern);
        jdbc.update("delete from incidents where reported_by in (select id from users where email like ?)", pattern);
        jdbc.update("delete from resources where call_sign like 'IT-%'");
        jdbc.update("delete from refresh_tokens where user_id in (select id from users where email like ?)", pattern);
        jdbc.update("delete from users where email like ?", pattern);
    }

    @Test
    void dispatchingToAVerifiedIncidentActivatesIt() {
        UUID incident = verifiedIncident();
        UUID ambulance = newAmbulance();

        AssignmentSummary assignment = dispatchService.dispatch(incident, ambulance, dispatcher, Role.DISPATCHER);

        assertThat(assignment.resourceId()).isEqualTo(ambulance);
        assertThat(assignment.incidentId()).isEqualTo(incident);
        assertThat(incidentService.get(incident).status()).isEqualTo(IncidentStatus.ACTIVE);
        assertThat(resourceService.get(ambulance).status()).isEqualTo(ResourceStatus.EN_ROUTE);
        assertThat(resourceAllocation.assignmentsFor(incident)).hasSize(1);
        assertThat(resourceAllocation.findAvailableNear(LATITUDE, LONGITUDE, 5, null))
                .noneMatch(found -> found.resource().id().equals(ambulance));
    }

    @Test
    void dispatchingIsRefusedForAnIncidentThatIsOnlyReported() {
        UUID incident = reportedIncident();
        UUID ambulance = newAmbulance();

        assertThatThrownBy(() -> dispatchService.dispatch(incident, ambulance, dispatcher, Role.DISPATCHER))
                .isInstanceOf(IncidentNotDispatchableException.class);

        assertThat(resourceService.get(ambulance).status()).isEqualTo(ResourceStatus.AVAILABLE);
        assertThat(resourceAllocation.assignmentsFor(incident)).isEmpty();
    }

    @Test
    void aResourceCannotBeSentToTwoIncidents() {
        UUID first = verifiedIncident();
        UUID second = verifiedIncident();
        UUID ambulance = newAmbulance();
        dispatchService.dispatch(first, ambulance, dispatcher, Role.DISPATCHER);

        assertThatThrownBy(() -> dispatchService.dispatch(second, ambulance, dispatcher, Role.DISPATCHER))
                .isInstanceOf(ResourceNotAvailableException.class);

        assertThat(incidentService.get(second).status()).isEqualTo(IncidentStatus.VERIFIED);
        assertThat(resourceAllocation.assignmentsFor(second)).isEmpty();
    }

    @Test
    void twoDispatchersAssigningTheSameResourceAtOnceLetExactlyOneThrough() throws Exception {
        UUID first = verifiedIncident();
        UUID second = verifiedIncident();
        UUID ambulance = newAmbulance();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            List<Future<Boolean>> attempts = new ArrayList<>();
            for (UUID incident : List.of(first, second)) {
                Callable<Boolean> attempt = () -> {
                    start.await();
                    try {
                        dispatchService.dispatch(incident, ambulance, dispatcher, Role.DISPATCHER);
                        return true;
                    } catch (ResourceNotAvailableException refused) {
                        return false;
                    }
                };
                attempts.add(pool.submit(attempt));
            }
            start.countDown();

            int succeeded = 0;
            for (Future<Boolean> attempt : attempts) {
                if (attempt.get(20, TimeUnit.SECONDS)) {
                    succeeded++;
                }
            }

            assertThat(succeeded).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }

        Integer openAssignments = jdbc.queryForObject(
                "select count(*) from assignments where resource_id = ? and released_at is null",
                Integer.class, ambulance);
        assertThat(openAssignments).isEqualTo(1);
        assertThat(resourceService.get(ambulance).status()).isEqualTo(ResourceStatus.EN_ROUTE);

        long activated = List.of(first, second).stream()
                .filter(incident -> incidentService.get(incident).status() == IncidentStatus.ACTIVE)
                .count();
        assertThat(activated).isEqualTo(1);
    }

    @Test
    void resolvingAnIncidentReleasesItsResources() {
        UUID incident = verifiedIncident();
        UUID ambulance = newAmbulance();
        dispatchService.dispatch(incident, ambulance, dispatcher, Role.DISPATCHER);

        incidentService.changeStatus(incident, IncidentStatus.CONTAINED, dispatcher, Role.DISPATCHER);
        assertThat(resourceService.get(ambulance).status()).isEqualTo(ResourceStatus.EN_ROUTE);

        incidentService.changeStatus(incident, IncidentStatus.RESOLVED, dispatcher, Role.DISPATCHER);

        assertThat(resourceService.get(ambulance).status()).isEqualTo(ResourceStatus.AVAILABLE);
        assertThat(resourceAllocation.assignmentsFor(incident))
                .hasSize(1)
                .allMatch(assignment -> assignment.releasedAt() != null);
    }

    @Test
    void dispatchAndReleaseAppearInTheIncidentHistory() {
        UUID incident = verifiedIncident();
        UUID ambulance = newAmbulance();
        AssignmentSummary assignment = dispatchService.dispatch(incident, ambulance, dispatcher, Role.DISPATCHER);
        resourceAllocation.release(assignment.id(), dispatcher, Role.DISPATCHER);

        List<AuditEntry> history = auditEntries.findIncidentHistory(incident);

        assertThat(history).extracting(AuditEntry::getAction).containsExactlyInAnyOrder(
                AuditAction.INCIDENT_REPORTED,
                AuditAction.INCIDENT_STATUS_CHANGED,
                AuditAction.RESOURCE_ASSIGNED,
                AuditAction.INCIDENT_STATUS_CHANGED,
                AuditAction.RESOURCE_RELEASED);

        AuditEntry assigned = history.stream()
                .filter(entry -> entry.getAction() == AuditAction.RESOURCE_ASSIGNED)
                .findFirst()
                .orElseThrow();
        assertThat(assigned.getEntityType()).isEqualTo(AuditEntry.RESOURCE);
        assertThat(assigned.getEntityId()).isEqualTo(ambulance);
        assertThat(assigned.getRelatedEntityId()).isEqualTo(incident);
        assertThat(assigned.getActorId()).isEqualTo(dispatcher);
        assertThat(assigned.getPreviousState()).isEqualTo("AVAILABLE");
        assertThat(assigned.getNewState()).isEqualTo("EN_ROUTE");
    }

    private UUID reportedIncident() {
        IncidentSummary incident = incidentService.report(new ReportIncidentRequest(
                "Flood in Plovdiv",
                "The Maritsa has burst its banks near the centre.",
                IncidentCategory.FLOOD,
                Severity.CRITICAL,
                LATITUDE,
                LONGITUDE,
                25000), dispatcher, Role.DISPATCHER);
        return incident.id();
    }

    private UUID verifiedIncident() {
        UUID incident = reportedIncident();
        incidentService.changeStatus(incident, IncidentStatus.VERIFIED, dispatcher, Role.DISPATCHER);
        return incident;
    }

    private UUID newAmbulance() {
        ResourceSummary resource = resourceService.create(new CreateResourceRequest(
                "IT-AMB-" + UUID.randomUUID().toString().substring(0, 8),
                ResourceType.AMBULANCE,
                LATITUDE + 0.01,
                LONGITUDE,
                null));
        return resource.id();
    }
}
