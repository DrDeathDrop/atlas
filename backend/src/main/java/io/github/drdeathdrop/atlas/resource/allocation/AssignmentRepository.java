package io.github.drdeathdrop.atlas.resource.allocation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    List<Assignment> findByIncidentIdAndReleasedAtIsNull(UUID incidentId);

    List<Assignment> findByIncidentIdOrderByAssignedAtAsc(UUID incidentId);

    @Query("select distinct a.incidentId from Assignment a where a.releasedAt is null")
    Set<UUID> findIncidentIdsWithOpenAssignments();
}
