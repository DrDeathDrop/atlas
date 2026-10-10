package io.github.drdeathdrop.atlas.resource.allocation;

import io.github.drdeathdrop.atlas.resource.NearbyResource;
import io.github.drdeathdrop.atlas.resource.ResourceAllocation;
import io.github.drdeathdrop.atlas.resource.ResourceStatus;
import io.github.drdeathdrop.atlas.resource.ResourceType;
import io.github.drdeathdrop.atlas.resource.inventory.CreateResourceRequest;
import io.github.drdeathdrop.atlas.resource.inventory.ResourceService;
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
class NearbyResourcesIntegrationTest {

    private static final double LATITUDE = 42.1354;
    private static final double LONGITUDE = 24.7453;
    private static final double DEGREES_PER_KILOMETER_NORTH = 1.0 / 111.2;

    @Autowired
    private ResourceAllocation allocation;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private JdbcTemplate jdbc;

    private String prefix;
    private String near;
    private String farther;
    private String fireTruck;

    @BeforeEach
    void setUp() {
        prefix = "IT-" + UUID.randomUUID().toString().substring(0, 8) + "-";
        near = createAt("AMB-NEAR", ResourceType.AMBULANCE, 5);
        farther = createAt("AMB-FARTHER", ResourceType.AMBULANCE, 12);
        fireTruck = createAt("FIRE-NEAR", ResourceType.FIRE_TRUCK, 8);
        createAt("AMB-TOO-FAR", ResourceType.AMBULANCE, 30);
        String busy = createAt("AMB-BUSY", ResourceType.AMBULANCE, 3);
        jdbc.update("update resources set status = ? where call_sign = ?", ResourceStatus.ON_SCENE.name(), busy);
    }

    @AfterEach
    void removeTestResources() {
        jdbc.update("delete from resources where call_sign like 'IT-%'");
    }

    @Test
    void findsOnlyAvailableResourcesInsideTheRadiusNearestFirst() {
        List<NearbyResource> found = mine(allocation.findAvailableNear(LATITUDE, LONGITUDE, 15, null));

        assertThat(found).extracting(result -> result.resource().callSign())
                .containsExactly(near, fireTruck, farther);
        assertThat(found.get(0).distanceMeters()).isBetween(4500.0, 5500.0);
        assertThat(found.get(1).distanceMeters()).isBetween(7500.0, 8500.0);
        assertThat(found.get(2).distanceMeters()).isBetween(11500.0, 12500.0);
    }

    @Test
    void canBeLimitedToOneTypeOfResource() {
        List<NearbyResource> found = mine(
                allocation.findAvailableNear(LATITUDE, LONGITUDE, 15, ResourceType.AMBULANCE));

        assertThat(found).extracting(result -> result.resource().callSign())
                .containsExactly(near, farther);
        assertThat(found).allSatisfy(result -> {
            assertThat(result.resource().type()).isEqualTo(ResourceType.AMBULANCE);
            assertThat(result.resource().status()).isEqualTo(ResourceStatus.AVAILABLE);
            assertThat(result.resource().latitude()).isGreaterThan(LATITUDE);
        });
    }

    @Test
    void aSmallerRadiusFindsFewerResources() {
        List<NearbyResource> found = mine(allocation.findAvailableNear(LATITUDE, LONGITUDE, 6, null));

        assertThat(found).extracting(result -> result.resource().callSign()).containsExactly(near);
    }

    private List<NearbyResource> mine(List<NearbyResource> all) {
        return all.stream()
                .filter(result -> result.resource().callSign().startsWith(prefix))
                .toList();
    }

    private String createAt(String name, ResourceType type, double kilometersNorth) {
        String callSign = prefix + name;
        resourceService.create(new CreateResourceRequest(
                callSign, type, LATITUDE + kilometersNorth * DEGREES_PER_KILOMETER_NORTH, LONGITUDE, null));
        return callSign;
    }
}
