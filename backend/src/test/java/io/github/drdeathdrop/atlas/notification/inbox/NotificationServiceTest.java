package io.github.drdeathdrop.atlas.notification.inbox;

import io.github.drdeathdrop.atlas.notification.NotificationsCreated;
import io.github.drdeathdrop.atlas.user.Role;
import io.github.drdeathdrop.atlas.user.UserDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Set<Role> COORDINATORS = Set.of(Role.ADMIN, Role.DISPATCHER);
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID OTHER_DISPATCHER = UUID.randomUUID();
    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID INCIDENT = UUID.randomUUID();

    @Mock
    private NotificationRepository repository;

    @Mock
    private UserDirectory users;

    @Mock
    private ApplicationEventPublisher events;

    @InjectMocks
    private NotificationService service;

    @Test
    void everyoneWithTheRoleIsNotifiedExceptWhoeverMadeTheChange() {
        when(users.enabledUserIdsWithRoles(COORDINATORS)).thenReturn(List.of(ACTOR, OTHER_DISPATCHER, ADMIN));

        service.notifyRoles(COORDINATORS, ACTOR, NotificationType.INCIDENT_REPORTED,
                "INC-2026-0001 was reported", INCIDENT);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> saved = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(Notification::getRecipientId)
                .containsExactly(OTHER_DISPATCHER, ADMIN);
        assertThat(saved.getValue()).allSatisfy(notification -> {
            assertThat(notification.getType()).isEqualTo(NotificationType.INCIDENT_REPORTED);
            assertThat(notification.getTitle()).isEqualTo("INC-2026-0001 was reported");
            assertThat(notification.getIncidentId()).isEqualTo(INCIDENT);
            assertThat(notification.isRead()).isFalse();
        });
        verify(events).publishEvent(new NotificationsCreated(2));
    }

    @Test
    void nothingIsStoredOrAnnouncedWhenNobodyElseHasTheRole() {
        when(users.enabledUserIdsWithRoles(COORDINATORS)).thenReturn(List.of(ACTOR));

        service.notifyRoles(COORDINATORS, ACTOR, NotificationType.INCIDENT_REPORTED,
                "INC-2026-0001 was reported", INCIDENT);

        verify(repository, never()).saveAll(anyList());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void markReadMarksTheUsersOwnNotification() {
        UUID id = UUID.randomUUID();
        Notification notification = new Notification(ADMIN, NotificationType.INCIDENT_REPORTED, "x", INCIDENT);
        when(repository.findByIdAndRecipientId(id, ADMIN)).thenReturn(Optional.of(notification));

        service.markRead(id, ADMIN);

        assertThat(notification.isRead()).isTrue();
    }

    @Test
    void markReadTreatsSomeoneElsesNotificationAsMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndRecipientId(id, ACTOR)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(id, ACTOR))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    void unreadCountComesFromTheRepository() {
        when(repository.countByRecipientIdAndReadAtIsNull(ADMIN)).thenReturn(4L);

        assertThat(service.unreadCountFor(ADMIN).count()).isEqualTo(4);
    }
}
