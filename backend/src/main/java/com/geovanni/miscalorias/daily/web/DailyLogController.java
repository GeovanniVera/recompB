package com.geovanni.miscalorias.daily.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geovanni.miscalorias.auth.security.JwtService.AuthenticatedUser;
import com.geovanni.miscalorias.daily.service.DailyLogService;
import com.geovanni.miscalorias.daily.web.dto.DailySummaryResponse;
import com.geovanni.miscalorias.daily.web.dto.OffPlanEntryRequest;
import com.geovanni.miscalorias.daily.web.dto.QuickPlateEntryRequest;
import com.geovanni.miscalorias.daily.web.dto.ScaleEntryRequest;
import com.geovanni.miscalorias.daily.web.dto.UpdateDailyLogRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/daily-logs")
@Tag(name = "Daily log", description = "Per-day food entries, hydration and gym flag")
public class DailyLogController {

    private final DailyLogService dailyLogService;

    public DailyLogController(DailyLogService dailyLogService) {
        this.dailyLogService = dailyLogService;
    }

    @GetMapping("/{date}")
    @Operation(summary = "Day summary, creating the log on first access")
    public DailySummaryResponse getDay(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dailyLogService.getDay(principal.id(), date);
    }

    @PostMapping("/{date}/entries/scale")
    @Operation(summary = "Log a food weighed on a scale")
    public DailySummaryResponse addScaleEntry(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody ScaleEntryRequest request) {
        return dailyLogService.addScaleEntry(principal.id(), date, request);
    }

    @PostMapping("/{date}/entries/quick-plate")
    @Operation(summary = "Log a quick plate combo")
    public DailySummaryResponse addQuickPlate(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody QuickPlateEntryRequest request) {
        return dailyLogService.addQuickPlateEntry(principal.id(), date, request);
    }

    @PostMapping("/{date}/entries/off-plan")
    @Operation(summary = "Log an off-plan craving with a label and calories")
    public DailySummaryResponse addOffPlan(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody OffPlanEntryRequest request) {
        return dailyLogService.addOffPlanEntry(principal.id(), date, request);
    }

    @DeleteMapping("/{date}/entries/{entryId}")
    @Operation(summary = "Delete an entry from the day")
    public DailySummaryResponse deleteEntry(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable Long entryId) {
        return dailyLogService.deleteEntry(principal.id(), date, entryId);
    }

    @PatchMapping("/{date}")
    @Operation(summary = "Update hydration and gym flag")
    public DailySummaryResponse updateDay(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody UpdateDailyLogRequest request) {
        return dailyLogService.updateDay(principal.id(), date, request);
    }
}
