package io.github.drdeathdrop.atlas.simulation.engine;

import io.github.drdeathdrop.atlas.simulation.SimulationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SimulationRunRepository extends JpaRepository<SimulationRun, UUID> {
    boolean existsByStatus(SimulationStatus status);

    List<SimulationRun> findTop20ByOrderByStartedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SimulationRun r where r.id = :id")
    Optional<SimulationRun> findByIdForUpdate(@Param("id") UUID id);

    @Query(value = """
            select *
            from simulation_runs
            where status = 'RUNNING'
              and next_incident_at <= :now
            order by next_incident_at
            for update skip locked
            """, nativeQuery = true)
    List<SimulationRun> lockDue(@Param("now") Instant now);

    @Modifying
    @Query(value = "insert into simulation_incidents (run_id, incident_id) values (:runId, :incidentId)",
            nativeQuery = true)
    void linkIncident(@Param("runId") UUID runId, @Param("incidentId") UUID incidentId);
}
