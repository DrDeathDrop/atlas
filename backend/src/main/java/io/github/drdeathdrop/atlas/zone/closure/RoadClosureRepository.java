package io.github.drdeathdrop.atlas.zone.closure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoadClosureRepository extends JpaRepository<RoadClosure, UUID> {
    List<RoadClosure> findByReopenedAtIsNullOrderByCreatedAtDesc();
}
