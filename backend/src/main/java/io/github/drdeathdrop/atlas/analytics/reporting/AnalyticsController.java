package io.github.drdeathdrop.atlas.analytics.reporting;

import io.github.drdeathdrop.atlas.analytics.AnalyticsOverview;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'DISPATCHER')")
    public AnalyticsOverview overview(@RequestParam(defaultValue = "30") int days,
                                      @RequestParam(defaultValue = "false") boolean includeSimulated) {
        return analyticsService.overview(days, includeSimulated);
    }
}
