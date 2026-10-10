package io.github.drdeathdrop.atlas.zone.area;

import io.github.drdeathdrop.atlas.shared.geo.GeoShapes;
import io.github.drdeathdrop.atlas.zone.ZoneChanged;
import io.github.drdeathdrop.atlas.zone.ZoneSummary;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ZoneService {
    private final ZoneRepository repository;
    private final ApplicationEventPublisher events;

    public ZoneService(ZoneRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @Transactional
    public ZoneSummary create(CreateZoneRequest request, UUID createdBy) {
        Zone zone = new Zone(
                request.name().trim(),
                request.type(),
                GeoShapes.polygon(request.boundary()),
                createdBy);

        Zone saved = repository.saveAndFlush(zone);
        events.publishEvent(new ZoneChanged(saved.getId()));

        return toSummary(saved);
    }

    @Transactional
    public ZoneSummary lift(UUID zoneId, UUID liftedBy) {
        Zone zone = repository.findById(zoneId)
                .orElseThrow(() -> new ZoneNotFoundException(zoneId));

        if (zone.isLifted()) {
            throw new ZoneAlreadyLiftedException(zoneId);
        }
        zone.lift(liftedBy, Instant.now());
        events.publishEvent(new ZoneChanged(zoneId));

        return toSummary(zone);
    }

    @Transactional(readOnly = true)
    public List<ZoneSummary> listActive() {
        return repository.findByLiftedAtIsNullOrderByCreatedAtDesc().stream()
                .map(ZoneService::toSummary)
                .toList();
    }

    private static ZoneSummary toSummary(Zone zone) {
        return new ZoneSummary(
                zone.getId(),
                zone.getName(),
                zone.getType(),
                GeoShapes.positions(zone.getBoundary()),
                zone.getCreatedAt(),
                zone.getLiftedAt());
    }
}
