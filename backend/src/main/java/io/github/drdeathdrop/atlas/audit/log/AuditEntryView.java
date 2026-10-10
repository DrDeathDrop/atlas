package io.github.drdeathdrop.atlas.audit.log;

import io.github.drdeathdrop.atlas.user.Role;

import java.time.Instant;
import java.util.UUID;

public record AuditEntryView(
        UUID id,
        Instant occurredAt,
        UUID actorId,
        Role actorRole,
        AuditAction action,
        String entityType,
        UUID entityId,
        String entityReference,
        String previousState,
        String newState,
        UUID relatedEntityId
) {
    static AuditEntryView of(AuditEntry entry) {
        return new AuditEntryView(
                entry.getId(),
                entry.getOccurredAt(),
                entry.getActorId(),
                entry.getActorRole(),
                entry.getAction(),
                entry.getEntityType(),
                entry.getEntityId(),
                entry.getEntityReference(),
                entry.getPreviousState(),
                entry.getNewState(),
                entry.getRelatedEntityId());
    }
}
