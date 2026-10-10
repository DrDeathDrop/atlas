package io.github.drdeathdrop.atlas.incident.priority;

import io.github.drdeathdrop.atlas.incident.IncidentCategory;
import io.github.drdeathdrop.atlas.incident.IncidentStatus;
import io.github.drdeathdrop.atlas.incident.Priority;
import io.github.drdeathdrop.atlas.incident.PriorityFactor;
import io.github.drdeathdrop.atlas.incident.PriorityLevel;
import io.github.drdeathdrop.atlas.incident.Severity;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class PriorityCalculator {
    static final int MAX_PEOPLE_POINTS = 25;
    static final int UNATTENDED_POINTS = 10;
    static final int MAX_WAITING_POINTS = 10;
    static final int MINUTES_PER_WAITING_POINT = 6;

    public Priority calculate(PriorityInput input, Instant now) {
        if (isClosed(input.status())) {
            return new Priority(input.incidentId(), 0, PriorityLevel.LOW, List.of(
                    new PriorityFactor("Status", 0, "Closed incidents are not prioritised")));
        }

        List<PriorityFactor> factors = new ArrayList<>();

        factors.add(new PriorityFactor("Severity", severityPoints(input.severity()),
                readable(input.severity().name()) + " severity"));

        factors.add(new PriorityFactor("People affected", peoplePoints(input.affectedPeople()),
                input.affectedPeople() + " people affected"));

        factors.add(new PriorityFactor("Category", categoryPoints(input.category()),
                readable(input.category().name()) + " incident"));

        long minutesWaiting = Math.max(0, Duration.between(input.reportedAt(), now).toMinutes());
        int waitingPoints = (int) Math.min(MAX_WAITING_POINTS, minutesWaiting / MINUTES_PER_WAITING_POINT);

        if (input.status() == IncidentStatus.REPORTED) {
            if (waitingPoints > 0) {
                factors.add(new PriorityFactor("Waiting", waitingPoints,
                        "Waiting " + minutesWaiting + " minutes to be verified"));
            }
        } else if (needsResources(input.status()) && !input.hasResources()) {
            factors.add(new PriorityFactor("Unattended", UNATTENDED_POINTS, "No resources assigned"));
            if (waitingPoints > 0) {
                factors.add(new PriorityFactor("Waiting", waitingPoints,
                        "Reported " + minutesWaiting + " minutes ago with nobody sent"));
            }
        }

        int total = factors.stream().mapToInt(PriorityFactor::points).sum();

        if (input.status() == IncidentStatus.CONTAINED) {
            int reduction = total - total / 2;
            factors.add(new PriorityFactor("Contained", -reduction, "The incident is contained"));
            total -= reduction;
        }

        int score = Math.clamp(total, 0, 100);

        return new Priority(input.incidentId(), score, PriorityLevel.of(score), List.copyOf(factors));
    }

    static int severityPoints(Severity severity) {
        return switch (severity) {
            case LOW -> 10;
            case MEDIUM -> 20;
            case HIGH -> 30;
            case CRITICAL -> 40;
        };
    }

    static int peoplePoints(int affectedPeople) {
        if (affectedPeople <= 0) {
            return 0;
        }
        return (int) Math.min(MAX_PEOPLE_POINTS, Math.round(5 * Math.log10(affectedPeople + 1.0)));
    }

    static int categoryPoints(IncidentCategory category) {
        return switch (category) {
            case EXPLOSION, BUILDING_COLLAPSE, CHEMICAL_LEAK, EARTHQUAKE, AVIATION_ACCIDENT -> 15;
            case STRUCTURE_FIRE, WILDFIRE, FLOOD, GAS_LEAK, AVALANCHE, LANDSLIDE, RAIL_ACCIDENT,
                 MEDICAL_EMERGENCY -> 10;
            case ROAD_ACCIDENT, SEVERE_WEATHER, SINKHOLE, MISSING_PERSON, WATER_SUPPLY_FAILURE,
                 POWER_OUTAGE -> 5;
            case OTHER -> 0;
        };
    }

    private static boolean isClosed(IncidentStatus status) {
        return status == IncidentStatus.RESOLVED
                || status == IncidentStatus.REJECTED
                || status == IncidentStatus.ARCHIVED;
    }

    private static boolean needsResources(IncidentStatus status) {
        return status == IncidentStatus.VERIFIED || status == IncidentStatus.ACTIVE;
    }

    private static String readable(String name) {
        String words = name.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
}
