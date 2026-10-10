package io.github.drdeathdrop.atlas.incident.core;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    @Query(value = "select nextval('incident_reference_seq')", nativeQuery = true)
    long nextReferenceNumber();
}
