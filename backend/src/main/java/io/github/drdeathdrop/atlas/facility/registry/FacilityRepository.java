package io.github.drdeathdrop.atlas.facility.registry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FacilityRepository extends JpaRepository<Facility, UUID> {
    boolean existsByName(String name);

    Optional<Facility> findByName(String name);
}
