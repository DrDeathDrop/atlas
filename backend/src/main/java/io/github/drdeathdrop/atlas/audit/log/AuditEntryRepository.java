package io.github.drdeathdrop.atlas.audit.log;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface AuditEntryRepository extends Repository<AuditEntry, UUID> {
    AuditEntry save(AuditEntry entry);

    List<AuditEntry> findByEntityTypeAndEntityIdOrderByOccurredAtAsc(String entityType, UUID entityId);
}
