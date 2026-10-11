package io.github.drdeathdrop.atlas.incident;

public record NewIncident(
        String title,
        String description,
        IncidentCategory category,
        Severity severity,
        double latitude,
        double longitude,
        int affectedPeople
) {
}
