package io.github.drdeathdrop.atlas.incident.core;

import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    @Query(value = "select nextval('incident_reference_seq')", nativeQuery = true)
    long nextReferenceNumber();

    List<Incident> findByStatusIn(Collection<IncidentStatus> statuses);
}
