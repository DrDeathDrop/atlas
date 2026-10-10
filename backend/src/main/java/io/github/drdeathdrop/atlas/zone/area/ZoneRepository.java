package io.github.drdeathdrop.atlas.zone.area;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ZoneRepository extends JpaRepository<Zone, UUID> {
    List<Zone> findByLiftedAtIsNullOrderByCreatedAtDesc();
}
