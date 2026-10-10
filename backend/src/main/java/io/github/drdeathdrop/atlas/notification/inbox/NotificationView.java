package io.github.drdeathdrop.atlas.notification.inbox;

import java.time.Instant;
import java.util.UUID;

public record NotificationView(
        UUID id,
        NotificationType type,
        String title,
        UUID incidentId,
        Instant createdAt,
        boolean read
) {
    static NotificationView of(Notification notification) {
        return new NotificationView(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getIncidentId(),
                notification.getCreatedAt(),
                notification.isRead());
    }
}
