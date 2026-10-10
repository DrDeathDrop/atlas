package io.github.drdeathdrop.atlas.resource.inventory;

import io.github.drdeathdrop.atlas.resource.ResourceType;
import io.github.drdeathdrop.atlas.shared.geo.GeoPoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@ConditionalOnProperty(name = "atlas.demo-data.enabled", havingValue = "true")
public class DemoResources implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoResources.class);

    private record Sample(String callSign, ResourceType type, double latitude, double longitude) {
    }

    private static final List<Sample> SAMPLES = List.of(
            new Sample("TEAM-ALPHA", ResourceType.RESCUE_TEAM, 42.1420, 24.7499),
            new Sample("TEAM-BRAVO", ResourceType.MEDICAL_TEAM, 42.1352, 24.7190),
            new Sample("TEAM-CHARLIE", ResourceType.FIRE_TEAM, 42.1561, 24.7412),
            new Sample("TEAM-DELTA", ResourceType.POLICE_UNIT, 42.1438, 24.7700),
            new Sample("TEAM-ECHO", ResourceType.TECHNICAL_TEAM, 42.1190, 24.7420),
            new Sample("AMBULANCE-17", ResourceType.AMBULANCE, 42.1408, 24.7626),
            new Sample("AMBULANCE-22", ResourceType.AMBULANCE, 42.1265, 24.7305),
            new Sample("AMBULANCE-31", ResourceType.AMBULANCE, 42.0125, 24.8774),
            new Sample("FIRETRUCK-04", ResourceType.FIRE_TRUCK, 42.1500, 24.7520),
            new Sample("FIRETRUCK-09", ResourceType.FIRE_TRUCK, 42.1300, 24.7900),
            new Sample("HELICOPTER-02", ResourceType.RESCUE_HELICOPTER, 42.0678, 24.8508),
            new Sample("RESCUE-BOAT-01", ResourceType.RESCUE_BOAT, 42.1525, 24.7450),
            new Sample("POLICE-CAR-12", ResourceType.POLICE_CAR, 42.1445, 24.7480),
            new Sample("UTILITY-06", ResourceType.UTILITY_VEHICLE, 42.1650, 24.7200),
            new Sample("TEAM-HOTEL", ResourceType.MEDICAL_TEAM, 42.0700, 24.7000),
            new Sample("FIRETRUCK-12", ResourceType.FIRE_TRUCK, 42.1700, 24.8400),
            new Sample("AMBULANCE-27", ResourceType.AMBULANCE, 42.2330, 24.7260),
            new Sample("UTILITY-11", ResourceType.UTILITY_VEHICLE, 42.1310, 24.9400),
            new Sample("FIRETRUCK-15", ResourceType.FIRE_TRUCK, 42.1340, 24.5350),
            new Sample("POLICE-CAR-20", ResourceType.POLICE_CAR, 42.2740, 24.9410),
            new Sample("TEAM-FOXTROT", ResourceType.RESCUE_TEAM, 42.1928, 24.3336),
            new Sample("TEAM-GOLF", ResourceType.MEDICAL_TEAM, 42.0990, 25.2240));

    private final ResourceRepository repository;

    public DemoResources(ResourceRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int added = 0;
        for (Sample sample : SAMPLES) {
            if (!repository.existsByCallSign(sample.callSign())) {
                repository.save(new Resource(
                        sample.callSign(),
                        sample.type(),
                        GeoPoints.of(sample.latitude(), sample.longitude()),
                        null));
                added++;
            }
        }
        if (added > 0) {
            log.info("Added {} sample resources", added);
        }
    }
}
