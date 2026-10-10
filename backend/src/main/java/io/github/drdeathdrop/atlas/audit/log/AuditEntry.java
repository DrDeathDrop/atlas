package io.github.drdeathdrop.atlas.audit.log;

import io.github.drdeathdrop.atlas.user.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Immutable
@Table(name = "audit_log")
public class AuditEntry {
    public static final String INCIDENT = "INCIDENT";
    public static final String RESOURCE = "RESOURCE";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(nullable = false, updatable = false)
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 32)
    private Role actorRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 64)
    private AuditAction action;

    @Column(nullable = false, updatable = false, length = 64)
    private String entityType;

    @Column(nullable = false, updatable = false)
    private UUID entityId;

    @Column(updatable = false, length = 64)
    private String entityReference;

    @Column(updatable = false, length = 64)
    private String previousState;

    @Column(updatable = false, length = 64)
    private String newState;

    @Column(updatable = false)
    private UUID relatedEntityId;

    protected AuditEntry() {
    }

    public AuditEntry(UUID actorId, Role actorRole, AuditAction action, String entityType, UUID entityId,
                      String entityReference, String previousState, String newState) {
        this(actorId, actorRole, action, entityType, entityId, entityReference, previousState, newState, null);
    }

    public AuditEntry(UUID actorId, Role actorRole, AuditAction action, String entityType, UUID entityId,
                      String entityReference, String previousState, String newState, UUID relatedEntityId) {
        this.occurredAt = Instant.now();
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.entityReference = entityReference;
        this.previousState = previousState;
        this.newState = newState;
        this.relatedEntityId = relatedEntityId;
    }
}
