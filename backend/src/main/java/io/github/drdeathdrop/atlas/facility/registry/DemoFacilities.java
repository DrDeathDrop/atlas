package io.github.drdeathdrop.atlas.facility.registry;

import io.github.drdeathdrop.atlas.facility.FacilityType;
import io.github.drdeathdrop.atlas.shared.geo.GeoPoints;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "atlas.demo-data.enabled", havingValue = "true")
public class DemoFacilities implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoFacilities.class);

    private record Sample(String name, FacilityType type, String address, double latitude, double longitude,
                          int capacity) {
    }

    private static final List<Sample> SAMPLES = List.of(
            new Sample("UMBAL Sveti Georgi", FacilityType.HOSPITAL, "bul. Peshtersko shose 66", 42.138716, 24.711595, 1500),
            new Sample("UMBAL Plovdiv", FacilityType.HOSPITAL, "bul. Bulgaria 234", 42.158500, 24.717338, 600),
            new Sample("UMBAL Kaspela", FacilityType.HOSPITAL, "ul. Sofia 64", 42.130002, 24.715984, 400),
            new Sample("MBAL Sveti Panteleymon", FacilityType.HOSPITAL, "bul. Nikola Vaptsarov 9", 42.121545, 24.750433, 300),
            new Sample("Kolodruma Arena", FacilityType.SHELTER, "bul. Asenovgradsko shose 8", 42.128556, 24.767302, 800),
            new Sample("International Fair Hall 6", FacilityType.SHELTER, "bul. Tsar Boris III Obedinitel 37", 42.157358, 24.749457, 1200),
            new Sample("Sports Hall Sila", FacilityType.SHELTER, "ul. Trakia 50", 42.137144, 24.739081, 500),
            new Sample("Plovdiv University Sports Hall", FacilityType.SHELTER, "bul. Bulgaria 236A", 42.156764, 24.715172, 300));

    private final FacilityRepository repository;

    public DemoFacilities(FacilityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int added = 0;
        int moved = 0;
        for (Sample sample : SAMPLES) {
            Point location = GeoPoints.of(sample.latitude(), sample.longitude());
            Optional<Facility> existing = repository.findByName(sample.name());

            if (existing.isEmpty()) {
                repository.save(new Facility(sample.name(), sample.type(), sample.address(), location, sample.capacity()));
                added++;
            } else if (!existing.get().getLocation().equalsExact(location)) {
                existing.get().relocate(location, sample.address());
                moved++;
            }
        }
        if (added > 0 || moved > 0) {
            log.info("Added {} sample facilities and corrected the location of {}", added, moved);
        }
    }
}
