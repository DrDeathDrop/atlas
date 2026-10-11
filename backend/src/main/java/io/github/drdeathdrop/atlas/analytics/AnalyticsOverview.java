package io.github.drdeathdrop.atlas.analytics;

import java.util.List;

public record AnalyticsOverview(
        int days,
        boolean includeSimulated,
        Totals totals,
        List<Count> byCategory,
        List<Count> bySeverity,
        List<Count> byStatus,
        List<DayCount> perDay,
        ResponseTimes responseTimes,
        List<Count> resourcesByStatus,
        List<Count> busiestResources,
        List<Hotspot> hotspots
) {
    public record Totals(long reported, long open, long closed, long simulated) {
    }

    public record Count(String name, long count) {
    }

    public record DayCount(String day, long count) {
    }

    public record TimeStat(long samples, Double averageMinutes, Double medianMinutes) {
    }

    public record ResponseTimes(TimeStat toVerify, TimeStat toFirstDispatch, TimeStat toResolve) {
    }

    public record Hotspot(double latitude, double longitude, long incidents) {
    }
}
