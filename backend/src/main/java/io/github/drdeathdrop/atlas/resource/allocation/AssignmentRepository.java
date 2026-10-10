package io.github.drdeathdrop.atlas.resource.allocation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    List<Assignment> findByIncidentIdAndReleasedAtIsNull(UUID incidentId);

    List<Assignment> findByIncidentIdOrderByAssignedAtAsc(UUID incidentId);
}
