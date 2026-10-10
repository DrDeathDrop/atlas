package io.github.drdeathdrop.atlas.zone.closure;

import io.github.drdeathdrop.atlas.shared.geo.GeoShapes;
import io.github.drdeathdrop.atlas.zone.RoadClosureChanged;
import io.github.drdeathdrop.atlas.zone.RoadClosureSummary;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RoadClosureService {
    private final RoadClosureRepository repository;
    private final ApplicationEventPublisher events;

    public RoadClosureService(RoadClosureRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @Transactional
    public RoadClosureSummary create(CreateRoadClosureRequest request, UUID createdBy) {
        String reason = request.reason() == null || request.reason().isBlank() ? null : request.reason().trim();

        RoadClosure closure = new RoadClosure(
                request.roadName().trim(),
                reason,
                GeoShapes.line(request.path()),
                createdBy);

        RoadClosure saved = repository.saveAndFlush(closure);
        events.publishEvent(new RoadClosureChanged(saved.getId()));

        return toSummary(saved);
    }

    @Transactional
    public RoadClosureSummary reopen(UUID closureId, UUID reopenedBy) {
        RoadClosure closure = repository.findById(closureId)
                .orElseThrow(() -> new RoadClosureNotFoundException(closureId));

        if (closure.isReopened()) {
            throw new RoadAlreadyReopenedException(closureId);
        }
        closure.reopen(reopenedBy, Instant.now());
        events.publishEvent(new RoadClosureChanged(closureId));

        return toSummary(closure);
    }

    @Transactional(readOnly = true)
    public List<RoadClosureSummary> listActive() {
        return repository.findByReopenedAtIsNullOrderByCreatedAtDesc().stream()
                .map(RoadClosureService::toSummary)
                .toList();
    }

    private static RoadClosureSummary toSummary(RoadClosure closure) {
        return new RoadClosureSummary(
                closure.getId(),
                closure.getRoadName(),
                closure.getReason(),
                GeoShapes.positions(closure.getPath()),
                closure.getCreatedAt(),
                closure.getReopenedAt());
    }
}
