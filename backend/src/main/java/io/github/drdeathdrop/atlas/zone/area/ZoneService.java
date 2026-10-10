package io.github.drdeathdrop.atlas.zone.area;

import io.github.drdeathdrop.atlas.shared.geo.GeoShapes;
import io.github.drdeathdrop.atlas.zone.ZoneSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ZoneService {
    private final ZoneRepository repository;

    public ZoneService(ZoneRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ZoneSummary create(CreateZoneRequest request, UUID createdBy) {
        Zone zone = new Zone(
                request.name().trim(),
                request.type(),
                GeoShapes.polygon(request.boundary()),
                createdBy);

        return toSummary(repository.saveAndFlush(zone));
    }

    @Transactional
    public ZoneSummary lift(UUID zoneId, UUID liftedBy) {
        Zone zone = repository.findById(zoneId)
                .orElseThrow(() -> new ZoneNotFoundException(zoneId));

        if (zone.isLifted()) {
            throw new ZoneAlreadyLiftedException(zoneId);
        }
        zone.lift(liftedBy, Instant.now());

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
