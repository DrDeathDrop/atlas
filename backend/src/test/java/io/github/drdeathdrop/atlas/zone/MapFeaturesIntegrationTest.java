package io.github.drdeathdrop.atlas.zone;

import io.github.drdeathdrop.atlas.facility.FacilitySummary;
import io.github.drdeathdrop.atlas.facility.FacilityType;
import io.github.drdeathdrop.atlas.facility.registry.CreateFacilityRequest;
import io.github.drdeathdrop.atlas.facility.registry.FacilityService;
import io.github.drdeathdrop.atlas.facility.registry.UpdateOccupancyRequest;
import io.github.drdeathdrop.atlas.shared.geo.GeoPosition;
import io.github.drdeathdrop.atlas.zone.area.CreateZoneRequest;
import io.github.drdeathdrop.atlas.zone.area.ZoneService;
import io.github.drdeathdrop.atlas.zone.closure.CreateRoadClosureRequest;
import io.github.drdeathdrop.atlas.zone.closure.RoadClosureService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MapFeaturesIntegrationTest {

    private static final List<GeoPosition> SQUARE = List.of(
            new GeoPosition(42.10, 24.70),
            new GeoPosition(42.10, 24.80),
            new GeoPosition(42.20, 24.80),
            new GeoPosition(42.20, 24.70));

    private static final List<GeoPosition> ROAD = List.of(
            new GeoPosition(42.14, 24.74),
            new GeoPosition(42.15, 24.75),
            new GeoPosition(42.16, 24.75));

    private static final UUID USER = UUID.randomUUID();

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private RoadClosureService roadClosureService;

    @Autowired
    private FacilityService facilityService;

    @Autowired
    private JdbcTemplate jdbc;

    private String name;

    @BeforeEach
    void setUp() {
        name = "IT-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @AfterEach
    void removeTestRows() {
        jdbc.update("delete from zones where name like 'IT-%'");
        jdbc.update("delete from road_closures where road_name like 'IT-%'");
        jdbc.update("delete from facilities where name like 'IT-%'");
    }

    @Test
    void aZoneKeepsItsCornersAndLeavesTheActiveListWhenLifted() {
        ZoneSummary created = zoneService.create(new CreateZoneRequest(name, ZoneType.EVACUATION, SQUARE), USER);

        assertThat(created.id()).isNotNull();
        assertThat(created.createdAt()).isNotNull();

        ZoneSummary listed = zoneService.listActive().stream()
                .filter(zone -> zone.id().equals(created.id()))
                .findFirst()
                .orElseThrow();
        assertThat(listed.boundary()).containsExactlyElementsOf(SQUARE);
        assertThat(listed.type()).isEqualTo(ZoneType.EVACUATION);

        Double squareKilometers = jdbc.queryForObject(
                "select ST_Area(cast(boundary as geography)) / 1000000 from zones where id = ?",
                Double.class, created.id());
        assertThat(squareKilometers).isBetween(85.0, 100.0);

        zoneService.lift(created.id(), USER);

        assertThat(zoneService.listActive()).extracting(ZoneSummary::id).doesNotContain(created.id());
    }

    @Test
    void aRoadClosureKeepsItsPathAndLeavesTheActiveListWhenReopened() {
        RoadClosureSummary created = roadClosureService.create(
                new CreateRoadClosureRequest(name, "Flooded underpass", ROAD), USER);

        RoadClosureSummary listed = roadClosureService.listActive().stream()
                .filter(closure -> closure.id().equals(created.id()))
                .findFirst()
                .orElseThrow();
        assertThat(listed.path()).containsExactlyElementsOf(ROAD);
        assertThat(listed.reason()).isEqualTo("Flooded underpass");

        roadClosureService.reopen(created.id(), USER);

        assertThat(roadClosureService.listActive()).extracting(RoadClosureSummary::id).doesNotContain(created.id());
    }

    @Test
    void aFacilityIsStoredWithItsLocationAndOccupancy() {
        FacilitySummary created = facilityService.create(
                new CreateFacilityRequest(name, FacilityType.SHELTER, "Test street 1", 42.1354, 24.7453, 200));

        facilityService.updateOccupancy(created.id(), new UpdateOccupancyRequest(120));

        FacilitySummary listed = facilityService.list().stream()
                .filter(facility -> facility.id().equals(created.id()))
                .findFirst()
                .orElseThrow();
        assertThat(listed.occupancy()).isEqualTo(120);
        assertThat(listed.capacity()).isEqualTo(200);
        assertThat(listed.latitude()).isEqualTo(42.1354);
        assertThat(listed.longitude()).isEqualTo(24.7453);
    }
}
