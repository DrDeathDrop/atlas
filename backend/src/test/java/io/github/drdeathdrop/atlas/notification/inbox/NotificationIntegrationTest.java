package io.github.drdeathdrop.atlas.notification.inbox;

import io.github.drdeathdrop.atlas.incident.IncidentCategory;
import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.incident.IncidentSummary;
import io.github.drdeathdrop.atlas.incident.Severity;
import io.github.drdeathdrop.atlas.incident.core.IncidentService;
import io.github.drdeathdrop.atlas.incident.core.ReportIncidentRequest;
import io.github.drdeathdrop.atlas.user.Role;
import io.github.drdeathdrop.atlas.user.account.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class NotificationIntegrationTest {

    private static final String EMAIL_SUFFIX = "@it.atlas.test";

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID reporter;
    private UUID colleague;
    private UUID viewer;

    @BeforeEach
    void setUp() {
        reporter = newUser(Role.DISPATCHER);
        colleague = newUser(Role.DISPATCHER);
        viewer = newUser(Role.VIEWER);
    }

    @AfterEach
    void removeTestData() {
        String pattern = "%" + EMAIL_SUFFIX;
        jdbc.update("delete from incidents where reported_by in (select id from users where email like ?)", pattern);
        jdbc.update("delete from refresh_tokens where user_id in (select id from users where email like ?)", pattern);
        jdbc.update("delete from users where email like ?", pattern);
    }

    @Test
    void reportingAnIncidentNotifiesOtherDispatchersButNotTheReporterOrViewers() {
        IncidentSummary incident = report();

        List<NotificationView> received = notificationService.latestFor(colleague);
        assertThat(received).hasSize(1);
        assertThat(received.get(0).type()).isEqualTo(NotificationType.INCIDENT_REPORTED);
        assertThat(received.get(0).title()).isEqualTo(incident.reference() + " was reported");
        assertThat(received.get(0).incidentId()).isEqualTo(incident.id());
        assertThat(received.get(0).read()).isFalse();
        assertThat(notificationService.unreadCountFor(colleague).count()).isEqualTo(1);

        assertThat(notificationService.latestFor(reporter)).isEmpty();
        assertThat(notificationService.latestFor(viewer)).isEmpty();
    }

    @Test
    void aStatusChangeNotifiesTheOtherDispatcher() {
        IncidentSummary incident = report();

        incidentService.changeStatus(incident.id(), IncidentStatus.VERIFIED, colleague, Role.DISPATCHER);

        assertThat(notificationService.latestFor(reporter))
                .extracting(NotificationView::title)
                .containsExactly(incident.reference() + " is now verified");
    }

    @Test
    void notificationsCanBeMarkedReadOnlyByTheirOwner() {
        report();
        UUID notification = notificationService.latestFor(colleague).get(0).id();

        assertThatThrownBy(() -> notificationService.markRead(notification, viewer))
                .isInstanceOf(NotificationNotFoundException.class);
        assertThat(notificationService.unreadCountFor(colleague).count()).isEqualTo(1);

        notificationService.markRead(notification, colleague);

        assertThat(notificationService.unreadCountFor(colleague).count()).isZero();
        assertThat(notificationService.latestFor(colleague).get(0).read()).isTrue();
    }

    @Test
    void everythingCanBeMarkedReadAtOnce() {
        report();
        report();
        assertThat(notificationService.unreadCountFor(colleague).count()).isEqualTo(2);

        notificationService.markAllRead(colleague);

        assertThat(notificationService.unreadCountFor(colleague).count()).isZero();
    }

    private IncidentSummary report() {
        return incidentService.report(new ReportIncidentRequest(
                "Integration test incident", "Created by a test.", IncidentCategory.FLOOD, Severity.LOW,
                42.1354, 24.7453, 0), reporter, Role.DISPATCHER);
    }

    private UUID newUser(Role role) {
        String email = role.name().toLowerCase() + "-" + UUID.randomUUID() + EMAIL_SUFFIX;
        return userService.createUser(email, "integration-test-1", "Test User", role).id();
    }
}
