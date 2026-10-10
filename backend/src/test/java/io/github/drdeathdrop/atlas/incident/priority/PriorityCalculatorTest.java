package io.github.drdeathdrop.atlas.incident.priority;

import io.github.drdeathdrop.atlas.incident.IncidentCategory;
import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.incident.Priority;
import io.github.drdeathdrop.atlas.incident.PriorityFactor;
import io.github.drdeathdrop.atlas.incident.PriorityLevel;
import io.github.drdeathdrop.atlas.incident.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PriorityCalculatorTest {

    private static final Instant NOW = Instant.parse("2026-10-11T12:00:00Z");
    private static final UUID INCIDENT = UUID.randomUUID();

    private final PriorityCalculator calculator = new PriorityCalculator();

    @ParameterizedTest
    @CsvSource({"LOW, 10", "MEDIUM, 20", "HIGH, 30", "CRITICAL, 40"})
    void moreSevereIncidentsScoreHigher(Severity severity, int expected) {
        assertThat(PriorityCalculator.severityPoints(severity)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"0, 0", "9, 5", "99, 10", "999, 15", "9999, 20", "99999, 25", "5000000, 25"})
    void peopleAffectedCountOnALogarithmicScale(int people, int expected) {
        assertThat(PriorityCalculator.peoplePoints(people)).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(IncidentCategory.class)
    void everyCategoryHasPoints(IncidentCategory category) {
        assertThat(PriorityCalculator.categoryPoints(category)).isBetween(0, 15);
    }

    @Test
    void theScoreIsTheSumOfItsFactors() {
        Priority priority = calculator.calculate(
                input(Severity.HIGH, IncidentCategory.FLOOD, IncidentStatus.ACTIVE, 999, Duration.ZERO, true), NOW);

        assertThat(priority.incidentId()).isEqualTo(INCIDENT);
        assertThat(priority.score()).isEqualTo(30 + 15 + 10);
        assertThat(priority.level()).isEqualTo(PriorityLevel.HIGH);
        assertThat(priority.factors()).extracting(PriorityFactor::name)
                .containsExactly("Severity", "People affected", "Category");
        assertThat(priority.factors()).extracting(PriorityFactor::points).containsExactly(30, 15, 10);
        assertThat(priority.factors().get(0).reason()).isEqualTo("High severity");
        assertThat(priority.factors().get(2).reason()).isEqualTo("Flood incident");
    }

    @Test
    void anIncidentNobodyHasBeenSentToScoresHigherTheLongerItWaits() {
        Priority attended = calculator.calculate(
                input(Severity.HIGH, IncidentCategory.FLOOD, IncidentStatus.VERIFIED, 999, Duration.ofMinutes(30), true),
                NOW);
        Priority unattended = calculator.calculate(
                input(Severity.HIGH, IncidentCategory.FLOOD, IncidentStatus.VERIFIED, 999, Duration.ofMinutes(30), false),
                NOW);
        Priority unattendedLonger = calculator.calculate(
                input(Severity.HIGH, IncidentCategory.FLOOD, IncidentStatus.VERIFIED, 999, Duration.ofHours(5), false),
                NOW);

        assertThat(attended.score()).isEqualTo(55);
        assertThat(unattended.score()).isEqualTo(55 + 10 + 5);
        assertThat(unattendedLonger.score()).isEqualTo(55 + 10 + 10);
        assertThat(unattended.factors()).extracting(PriorityFactor::name).contains("Unattended", "Waiting");
    }

    @Test
    void anUnverifiedReportGainsPointsWhileItWaitsButIsNotCalledUnattended() {
        Priority priority = calculator.calculate(
                input(Severity.LOW, IncidentCategory.OTHER, IncidentStatus.REPORTED, 0, Duration.ofMinutes(18), false),
                NOW);

        assertThat(priority.score()).isEqualTo(10 + 3);
        assertThat(priority.factors()).extracting(PriorityFactor::name)
                .contains("Waiting")
                .doesNotContain("Unattended");
    }

    @Test
    void aContainedIncidentScoresHalf() {
        Priority priority = calculator.calculate(
                input(Severity.CRITICAL, IncidentCategory.EXPLOSION, IncidentStatus.CONTAINED, 99999,
                        Duration.ofHours(2), false),
                NOW);

        assertThat(priority.score()).isEqualTo(40);
        assertThat(priority.factors()).extracting(PriorityFactor::name)
                .contains("Contained")
                .doesNotContain("Unattended", "Waiting");
        assertThat(priority.factors().stream().mapToInt(PriorityFactor::points).sum()).isEqualTo(priority.score());
    }

    @ParameterizedTest
    @EnumSource(value = IncidentStatus.class, names = {"RESOLVED", "REJECTED", "ARCHIVED"})
    void closedIncidentsScoreZero(IncidentStatus status) {
        Priority priority = calculator.calculate(
                input(Severity.CRITICAL, IncidentCategory.EXPLOSION, status, 99999, Duration.ofHours(2), false), NOW);

        assertThat(priority.score()).isZero();
        assertThat(priority.level()).isEqualTo(PriorityLevel.LOW);
        assertThat(priority.factors()).hasSize(1);
    }

    @Test
    void theWorstCaseIsExactlyOneHundred() {
        Priority priority = calculator.calculate(
                input(Severity.CRITICAL, IncidentCategory.EXPLOSION, IncidentStatus.ACTIVE, 99999,
                        Duration.ofHours(3), false),
                NOW);

        assertThat(priority.score()).isEqualTo(100);
        assertThat(priority.level()).isEqualTo(PriorityLevel.URGENT);
    }

    @ParameterizedTest
    @CsvSource({"0, LOW", "24, LOW", "25, ELEVATED", "49, ELEVATED", "50, HIGH", "74, HIGH", "75, URGENT", "100, URGENT"})
    void levelsFollowTheScore(int score, PriorityLevel expected) {
        assertThat(PriorityLevel.of(score)).isEqualTo(expected);
    }

    private static PriorityInput input(Severity severity, IncidentCategory category, IncidentStatus status,
                                       int people, Duration age, boolean hasResources) {
        return new PriorityInput(INCIDENT, severity, category, status, people, NOW.minus(age), hasResources);
    }
}
