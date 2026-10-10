package io.github.drdeathdrop.atlas.resource.inventory;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {
    boolean existsByCallSign(String callSign);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select r from Resource r where r.id = :id")
    Optional<Resource> findByIdForUpdate(@Param("id") UUID id);

    @Query(value = """
            select r.id                 as "id",
                   r.call_sign          as "callSign",
                   r.type               as "type",
                   r.status             as "status",
                   ST_Y(r.location)     as "latitude",
                   ST_X(r.location)     as "longitude",
                   r.team_id            as "teamId",
                   ST_Distance(
                       cast(r.location as geography),
                       cast(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) as geography)
                   )                    as "distanceMeters"
            from resources r
            where r.status = 'AVAILABLE'
              and (cast(:type as varchar) is null or r.type = cast(:type as varchar))
              and ST_DWithin(
                      cast(r.location as geography),
                      cast(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) as geography),
                      :radiusMeters
                  )
            order by "distanceMeters"
            """, nativeQuery = true)
    List<NearbyResourceRow> findAvailableNear(@Param("latitude") double latitude,
                                              @Param("longitude") double longitude,
                                              @Param("radiusMeters") double radiusMeters,
                                              @Param("type") String type);
}
