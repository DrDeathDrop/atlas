package io.github.drdeathdrop.atlas.incident.priority;

import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.incident.Priority;
import io.github.drdeathdrop.atlas.incident.core.Incident;
import io.github.drdeathdrop.atlas.incident.core.IncidentNotFoundException;
import io.github.drdeathdrop.atlas.incident.core.IncidentRepository;
import io.github.drdeathdrop.atlas.resource.ResourceAllocation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PriorityService {
    static final List<IncidentStatus> OPEN = List.of(
            IncidentStatus.REPORTED, IncidentStatus.VERIFIED, IncidentStatus.ACTIVE, IncidentStatus.CONTAINED);

    private final IncidentRepository incidents;
    private final ResourceAllocation resourceAllocation;
    private final PriorityCalculator calculator;

    public PriorityService(IncidentRepository incidents, ResourceAllocation resourceAllocation,
                           PriorityCalculator calculator) {
        this.incidents = incidents;
        this.resourceAllocation = resourceAllocation;
        this.calculator = calculator;
    }

    @Transactional(readOnly = true)
    public List<Priority> openIncidentsByPriority() {
        Set<UUID> attended = resourceAllocation.incidentsWithOpenAssignments();
        Instant now = Instant.now();

        return incidents.findByStatusIn(OPEN).stream()
                .sorted(Comparator.comparing(Incident::getCreatedAt))
                .map(incident -> calculator.calculate(toInput(incident, attended), now))
                .sorted(Comparator.comparingInt(Priority::score).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public Priority forIncident(UUID incidentId) {
        Incident incident = incidents.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        return calculator.calculate(toInput(incident, resourceAllocation.incidentsWithOpenAssignments()),
                Instant.now());
    }

    private static PriorityInput toInput(Incident incident, Set<UUID> attended) {
        return new PriorityInput(
                incident.getId(),
                incident.getSeverity(),
                incident.getCategory(),
                incident.getStatus(),
                incident.getAffectedPeople(),
                incident.getCreatedAt(),
                attended.contains(incident.getId()));
    }
}
