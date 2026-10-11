package io.github.drdeathdrop.atlas.analytics.reporting;

import io.github.drdeathdrop.atlas.analytics.AnalyticsOverview;
import io.github.drdeathdrop.atlas.analytics.AnalyticsOverview.ResponseTimes;
import io.github.drdeathdrop.atlas.analytics.reporting.AnalyticsQueries.Grouping;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
public class AnalyticsService {
    static final int MIN_DAYS = 1;
    static final int MAX_DAYS = 365;
    static final int BUSIEST_RESOURCES = 5;
    static final int HOTSPOTS = 10;
    static final double HOTSPOT_CELL_DEGREES = 0.02;

    private final AnalyticsQueries queries;

    public AnalyticsService(AnalyticsQueries queries) {
        this.queries = queries;
    }

    @Transactional(readOnly = true)
    public AnalyticsOverview overview(int requestedDays, boolean includeSimulated) {
        return overview(requestedDays, includeSimulated, LocalDate.now(ZoneOffset.UTC));
    }

    AnalyticsOverview overview(int requestedDays, boolean includeSimulated, LocalDate today) {
        int days = Math.max(MIN_DAYS, Math.min(MAX_DAYS, requestedDays));
        LocalDate firstDay = today.minusDays(days - 1L);
        Instant since = firstDay.atStartOfDay(ZoneOffset.UTC).toInstant();

        return new AnalyticsOverview(
                days,
                includeSimulated,
                queries.totals(since, includeSimulated),
                queries.incidentsBy(Grouping.CATEGORY, since, includeSimulated),
                queries.incidentsBy(Grouping.SEVERITY, since, includeSimulated),
                queries.incidentsBy(Grouping.STATUS, since, includeSimulated),
                queries.incidentsPerDay(firstDay, today, since, includeSimulated),
                new ResponseTimes(
                        queries.timeToStatus("VERIFIED", since, includeSimulated),
                        queries.timeToFirstDispatch(since, includeSimulated),
                        queries.timeToStatus("RESOLVED", since, includeSimulated)),
                queries.resourcesByStatus(),
                queries.busiestResources(since, BUSIEST_RESOURCES),
                queries.hotspots(since, includeSimulated, HOTSPOT_CELL_DEGREES, HOTSPOTS));
    }
}
