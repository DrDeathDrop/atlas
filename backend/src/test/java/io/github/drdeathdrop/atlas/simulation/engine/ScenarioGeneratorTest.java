package io.github.drdeathdrop.atlas.simulation.engine;

import io.github.drdeathdrop.atlas.incident.IncidentCategory;
import io.github.drdeathdrop.atlas.incident.NewIncident;
import io.github.drdeathdrop.atlas.incident.Severity;
import io.github.drdeathdrop.atlas.simulation.Scenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioGeneratorTest {

    private static final double LATITUDE = 42.1354;
    private static final double LONGITUDE = 24.7453;
    private static final double RADIUS_KM = 8;

    private final ScenarioGenerator generator = new ScenarioGenerator();

    @Test
    void theSameSeedAndIndexAlwaysGiveTheSameIncident() {
        NewIncident first = generator.generate(Scenario.FLOOD, LATITUDE, LONGITUDE, RADIUS_KM, true, 42, 3);
        NewIncident second = generator.generate(Scenario.FLOOD, LATITUDE, LONGITUDE, RADIUS_KM, true, 42, 3);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void differentIndexesGiveDifferentPlaces() {
        Set<String> places = new HashSet<>();
        IntStream.range(0, 30).forEach(index -> {
            NewIncident incident = generator.generate(Scenario.FLOOD, LATITUDE, LONGITUDE, RADIUS_KM, true, 42, index);
            places.add(incident.latitude() + "," + incident.longitude());
        });

        assertThat(places).hasSizeGreaterThan(25);
    }

    @ParameterizedTest
    @EnumSource(Scenario.class)
    void everyIncidentLiesInsideTheRadiusAndFitsItsScenario(Scenario scenario) {
        for (int index = 0; index < 200; index++) {
            NewIncident incident = generator.generate(scenario, LATITUDE, LONGITUDE, RADIUS_KM, true, 7, index);

            double northKm = (incident.latitude() - LATITUDE) * ScenarioGenerator.KILOMETERS_PER_DEGREE_LATITUDE;
            double eastKm = (incident.longitude() - LONGITUDE)
                    * ScenarioGenerator.KILOMETERS_PER_DEGREE_LATITUDE * Math.cos(Math.toRadians(LATITUDE));

            assertThat(Math.hypot(northKm, eastKm)).isLessThanOrEqualTo(RADIUS_KM + 0.01);
            assertThat(ScenarioGenerator.possibleCategories(scenario)).contains(incident.category());
            assertThat(incident.title()).startsWith("Simulated ").endsWith("#" + (index + 1));
            assertThat(incident.title().length()).isLessThanOrEqualTo(200);
            assertThat(incident.description()).contains("not a real incident");
            assertThat(incident.affectedPeople()).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    void moreSevereIncidentsAffectMorePeople() {
        for (int index = 0; index < 300; index++) {
            NewIncident incident = generator.generate(Scenario.EARTHQUAKE, LATITUDE, LONGITUDE, RADIUS_KM, true, 99, index);

            if (incident.severity() == Severity.LOW) {
                assertThat(incident.affectedPeople()).isLessThan(10);
            }
            if (incident.severity() == Severity.CRITICAL) {
                assertThat(incident.affectedPeople()).isBetween(500, 9999);
            }
        }
    }

    @Test
    void aScenarioProducesAMixOfSeverities() {
        Set<Severity> seen = new HashSet<>();
        IntStream.range(0, 300).forEach(index -> seen.add(
                generator.generate(Scenario.WILDFIRE, LATITUDE, LONGITUDE, RADIUS_KM, true, 5, index).severity()));

        assertThat(seen).containsExactlyInAnyOrder(Severity.values());
    }

    @Test
    void coordinatesStayValidNearThePole() {
        NewIncident incident = generator.generate(Scenario.SEVERE_STORM, 89.99, 179.99, 100, true, 1, 0);

        assertThat(incident.latitude()).isBetween(-90.0, 90.0);
        assertThat(incident.longitude()).isBetween(-180.0, 180.0);
    }

    @ParameterizedTest
    @EnumSource(value = Scenario.class, names = "RANDOM", mode = EnumSource.Mode.EXCLUDE)
    void withoutRelatedIncidentsEveryIncidentIsOfTheScenariosOwnKind(Scenario scenario) {
        for (int index = 0; index < 100; index++) {
            NewIncident incident = generator.generate(scenario, LATITUDE, LONGITUDE, RADIUS_KM, false, 7, index);

            assertThat(incident.category()).isEqualTo(ScenarioGenerator.mainCategory(scenario));
        }
    }

    @Test
    void theOwnKindOfAFloodIsAFlood() {
        assertThat(ScenarioGenerator.mainCategory(Scenario.FLOOD)).isEqualTo(IncidentCategory.FLOOD);
        assertThat(ScenarioGenerator.mainCategory(Scenario.EARTHQUAKE)).isEqualTo(IncidentCategory.EARTHQUAKE);
        assertThat(ScenarioGenerator.mainCategory(Scenario.WILDFIRE)).isEqualTo(IncidentCategory.WILDFIRE);
        assertThat(ScenarioGenerator.mainCategory(Scenario.SEVERE_STORM)).isEqualTo(IncidentCategory.SEVERE_WEATHER);
    }

    @Test
    void theRandomScenarioProducesManyKindsButNeverOther() {
        Set<IncidentCategory> seen = new HashSet<>();
        IntStream.range(0, 400).forEach(index -> seen.add(
                generator.generate(Scenario.RANDOM, LATITUDE, LONGITUDE, RADIUS_KM, false, 3, index).category()));

        assertThat(seen).hasSizeGreaterThan(10).doesNotContain(IncidentCategory.OTHER);
    }

    @Test
    void withRelatedIncidentsAScenarioProducesSeveralKinds() {
        Set<IncidentCategory> seen = new HashSet<>();
        IntStream.range(0, 200).forEach(index -> seen.add(
                generator.generate(Scenario.FLOOD, LATITUDE, LONGITUDE, RADIUS_KM, true, 3, index).category()));

        assertThat(seen).hasSizeGreaterThan(1).contains(IncidentCategory.FLOOD);
    }
}
