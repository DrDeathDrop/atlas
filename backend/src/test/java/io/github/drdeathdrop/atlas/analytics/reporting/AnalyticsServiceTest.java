package io.github.drdeathdrop.atlas.analytics.reporting;

import io.github.drdeathdrop.atlas.analytics.AnalyticsOverview;
import io.github.drdeathdrop.atlas.analytics.AnalyticsOverview.Count;
import io.github.drdeathdrop.atlas.analytics.AnalyticsOverview.TimeStat;
import io.github.drdeathdrop.atlas.analytics.AnalyticsOverview.Totals;
import io.github.drdeathdrop.atlas.analytics.reporting.AnalyticsQueries.Grouping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    private static final LocalDate TODAY = LocalDate.parse("2026-10-11");

    @Mock
    private AnalyticsQueries queries;

    @InjectMocks
    private AnalyticsService service;

    @Test
    void thePeriodEndsTodayAndCoversTheRequestedNumberOfDays() {
        Instant since = Instant.parse("2026-10-05T00:00:00Z");
        when(queries.totals(since, false)).thenReturn(new Totals(12, 5, 7, 3));
        when(queries.incidentsBy(Grouping.CATEGORY, since, false)).thenReturn(List.of(new Count("FLOOD", 9)));
        when(queries.timeToStatus("VERIFIED", since, false)).thenReturn(new TimeStat(4, 6.5, 5.0));

        AnalyticsOverview overview = service.overview(7, false, TODAY);

        assertThat(overview.days()).isEqualTo(7);
        assertThat(overview.includeSimulated()).isFalse();
        assertThat(overview.totals().reported()).isEqualTo(12);
        assertThat(overview.byCategory()).containsExactly(new Count("FLOOD", 9));
        assertThat(overview.responseTimes().toVerify().medianMinutes()).isEqualTo(5.0);
        verify(queries).incidentsPerDay(LocalDate.parse("2026-10-05"), TODAY, since, false);
        verify(queries).timeToStatus("RESOLVED", since, false);
        verify(queries).timeToFirstDispatch(since, false);
        verify(queries).hotspots(eq(since), eq(false), anyDouble(), anyInt());
    }

    @Test
    void aSingleDayStartsAtMidnightToday() {
        service.overview(1, true, TODAY);

        Instant midnight = Instant.parse("2026-10-11T00:00:00Z");
        verify(queries).totals(midnight, true);
        verify(queries).incidentsPerDay(TODAY, TODAY, midnight, true);
    }

    @Test
    void thePeriodIsLimitedToASensibleRange() {
        assertThat(service.overview(0, false, TODAY).days()).isEqualTo(1);
        assertThat(service.overview(-5, false, TODAY).days()).isEqualTo(1);
        assertThat(service.overview(5000, false, TODAY).days()).isEqualTo(365);

        verify(queries).incidentsPerDay(eq(LocalDate.parse("2025-10-12")), eq(TODAY), any(Instant.class), anyBoolean());
    }

    @Test
    void resourceFiguresDoNotDependOnSimulatedIncidents() {
        when(queries.resourcesByStatus()).thenReturn(List.of(new Count("AVAILABLE", 20)));

        AnalyticsOverview overview = service.overview(30, true, TODAY);

        assertThat(overview.resourcesByStatus()).containsExactly(new Count("AVAILABLE", 20));
        verify(queries).busiestResources(any(Instant.class), eq(AnalyticsService.BUSIEST_RESOURCES));
        verify(queries, org.mockito.Mockito.never()).timeToStatus(anyString(), any(Instant.class), eq(false));
    }
}
