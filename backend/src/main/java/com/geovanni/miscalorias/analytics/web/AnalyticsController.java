package com.geovanni.miscalorias.analytics.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.geovanni.miscalorias.analytics.service.AnalyticsService;
import com.geovanni.miscalorias.analytics.web.dto.FourteenDayResponse;
import com.geovanni.miscalorias.analytics.web.dto.WeekComparisonResponse;
import com.geovanni.miscalorias.auth.security.JwtService.AuthenticatedUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Fortnight metrics and week-over-week comparison")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/14-days")
    @Operation(summary = "Day-by-day series for the last 14 days plus previous vs current week")
    public FourteenDayResponse fourteenDays(@AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return analyticsService.getFourteenDays(principal.id(), endDate);
    }

    @GetMapping("/compare-weeks")
    @Operation(summary = "Compare any 7-day block against another 7-day block")
    public WeekComparisonResponse compareWeeks(@AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate currentStart) {
        return analyticsService.compareWeeks(principal.id(), baseStart, currentStart);
    }
}
