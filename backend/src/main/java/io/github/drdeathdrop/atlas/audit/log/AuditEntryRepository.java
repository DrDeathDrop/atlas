package io.github.drdeathdrop.atlas.audit.log;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AuditEntryRepository extends Repository<AuditEntry, UUID> {
    AuditEntry save(AuditEntry entry);

    List<AuditEntry> findByEntityTypeAndEntityIdOrderByOccurredAtAsc(String entityType, UUID entityId);

    @Query("""
            select e from AuditEntry e
            where (e.entityType = 'INCIDENT' and e.entityId = :incidentId)
               or e.relatedEntityId = :incidentId
            order by e.occurredAt asc
            """)
    List<AuditEntry> findIncidentHistory(@Param("incidentId") UUID incidentId);
}
