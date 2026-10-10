package io.github.drdeathdrop.atlas.notification.inbox;

import io.github.drdeathdrop.atlas.notification.NotificationsCreated;
import io.github.drdeathdrop.atlas.user.Role;
import io.github.drdeathdrop.atlas.user.UserDirectory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {
    private final NotificationRepository repository;
    private final UserDirectory users;
    private final ApplicationEventPublisher events;

    public NotificationService(NotificationRepository repository, UserDirectory users,
                               ApplicationEventPublisher events) {
        this.repository = repository;
        this.users = users;
        this.events = events;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyRoles(Collection<Role> roles, UUID exceptUser, NotificationType type, String title,
                            UUID incidentId) {
        List<Notification> notifications = users.enabledUserIdsWithRoles(roles).stream()
                .filter(recipient -> !recipient.equals(exceptUser))
                .map(recipient -> new Notification(recipient, type, title, incidentId))
                .toList();

        if (notifications.isEmpty()) {
            return;
        }

        repository.saveAll(notifications);
        events.publishEvent(new NotificationsCreated(notifications.size()));
    }

    @Transactional(readOnly = true)
    public List<NotificationView> latestFor(UUID recipientId) {
        return repository.findTop30ByRecipientIdOrderByCreatedAtDesc(recipientId).stream()
                .map(NotificationView::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public UnreadCount unreadCountFor(UUID recipientId) {
        return new UnreadCount(repository.countByRecipientIdAndReadAtIsNull(recipientId));
    }

    @Transactional
    public void markRead(UUID notificationId, UUID recipientId) {
        Notification notification = repository.findByIdAndRecipientId(notificationId, recipientId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        notification.markRead(Instant.now());
    }

    @Transactional
    public void markAllRead(UUID recipientId) {
        repository.markAllRead(recipientId, Instant.now());
    }
}
