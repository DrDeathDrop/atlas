package io.github.drdeathdrop.atlas.incident.core;

import ch.qos.logback.core.status.Status;
import io.github.drdeathdrop.atlas.incident.IncidentReported;
import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.incident.IncidentStatusChanged;
import io.github.drdeathdrop.atlas.incident.IncidentSummary;
import io.github.drdeathdrop.atlas.incident.lifecycle.IncidentLifecycle;
import io.github.drdeathdrop.atlas.shared.geo.GeoPoints;
import io.github.drdeathdrop.atlas.user.Role;
import org.locationtech.jts.geom.Point;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
public class IncidentService {
    private final IncidentRepository repository;
    private final IncidentLifecycle lifecycle;
    private final ApplicationEventPublisher events;

    public IncidentService(IncidentRepository repository, IncidentLifecycle lifecycle,
                           ApplicationEventPublisher events) {
        this.repository = repository;
        this.lifecycle = lifecycle;
        this.events = events;
    }

    @Transactional
    public IncidentSummary report(ReportIncidentRequest request, UUID reportedBy) {
        long number = repository.nextReferenceNumber();
        String reference = String.format("INC-%d-%04d", Year.now().getValue(), number);

        Point location = GeoPoints.of(request.latitude(), request.longitude());

        Incident incident = new Incident(
                reference,
                request.title(),
                request.description(),
                request.category(),
                request.severity(),
                location,
                request.affectedPeople(),
                reportedBy);

        Incident saved = repository.saveAndFlush(incident);

        events.publishEvent(new IncidentReported(saved.getId(), saved.getReference(), saved.getReportedBy()));

        return toSummary(saved);
    }

    @Transactional
    public IncidentSummary changeStatus(UUID incidentId, IncidentStatus newStatus, UUID changedBy, Role role) {
        Incident incident = repository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        IncidentStatus previous = incident.getStatus();

        lifecycle.validate(role, previous, newStatus);

        incident.moveTo(newStatus);

        events.publishEvent(new IncidentStatusChanged(
                incident.getId(), incident.getReference(), previous, newStatus, changedBy));

        return toSummary(incident);
    }

    @Transactional(readOnly = true)
    public IncidentSummary get(UUID incidentId) {
        return repository.findById(incidentId)
                .map(IncidentService::toSummary)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));
    }

    @Transactional(readOnly = true)
    public List<IncidentSummary> list() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(IncidentService::toSummary)
                .toList();
    }

    static IncidentSummary toSummary(Incident incident) {
        return new IncidentSummary(
                incident.getId(),
                incident.getReference(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getCategory(),
                incident.getSeverity(),
                incident.getStatus(),
                GeoPoints.latitude(incident.getLocation()),
                GeoPoints.longitude(incident.getLocation()),
                incident.getAffectedPeople(),
                incident.getReportedBy(),
                incident.getCreatedAt(),
                incident.getUpdatedAt());
    }
}
