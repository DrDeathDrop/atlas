package io.github.drdeathdrop.atlas.facility.registry;

import io.github.drdeathdrop.atlas.facility.FacilityType;
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
public class DemoFacilities implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoFacilities.class);

    private record Sample(String name, FacilityType type, String address, double latitude, double longitude,
                          int capacity) {
    }

    private static final List<Sample> SAMPLES = List.of(
            new Sample("UMBAL Sveti Georgi", FacilityType.HOSPITAL, "Peshtersko shose 66", 42.1327, 24.7106, 1500),
            new Sample("UMBAL Plovdiv", FacilityType.HOSPITAL, "bul. Bulgaria 234", 42.1639, 24.7262, 600),
            new Sample("UMBAL Kaspela", FacilityType.HOSPITAL, "ul. Sofia 64", 42.1557, 24.7106, 400),
            new Sample("MBAL Sveti Panteleymon", FacilityType.HOSPITAL, "bul. Nikola Vaptsarov 9", 42.1296, 24.7556, 300),
            new Sample("Kolodruma Arena", FacilityType.SHELTER, "Asenovgradsko shose", 42.1247, 24.7745, 800),
            new Sample("International Fair Hall 6", FacilityType.SHELTER, "bul. Tsar Boris III Obedinitel 37", 42.1570, 24.7530, 1200),
            new Sample("Sports Hall Sila", FacilityType.SHELTER, "ul. Yasna Polyana", 42.1407, 24.7190, 500),
            new Sample("Plovdiv University Sports Hall", FacilityType.SHELTER, "bul. Bulgaria 236", 42.1650, 24.7300, 300));

    private final FacilityRepository repository;

    public DemoFacilities(FacilityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int added = 0;
        for (Sample sample : SAMPLES) {
            if (!repository.existsByName(sample.name())) {
                repository.save(new Facility(
                        sample.name(),
                        sample.type(),
                        sample.address(),
                        GeoPoints.of(sample.latitude(), sample.longitude()),
                        sample.capacity()));
                added++;
            }
        }
        if (added > 0) {
            log.info("Added {} sample facilities", added);
        }
    }
}
