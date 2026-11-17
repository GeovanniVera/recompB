package com.geovanni.miscalorias.analytics.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.geovanni.miscalorias.analytics.service.AnalyticsService;
import com.geovanni.miscalorias.analytics.web.dto.CheckInResponse;
import com.geovanni.miscalorias.analytics.web.dto.CheckInResult;
import com.geovanni.miscalorias.analytics.web.dto.CreateCheckInRequest;
import com.geovanni.miscalorias.auth.security.JwtService.AuthenticatedUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/checkins")
@Tag(name = "Check-ins", description = "Fortnightly fasted weight and waist measurements")
public class CheckInController {

    private final AnalyticsService analyticsService;

    public CheckInController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    @Operation(summary = "Check-in history, oldest first")
    public List<CheckInResponse> list(@AuthenticationPrincipal AuthenticatedUser principal) {
        return analyticsService.listCheckIns(principal.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a check-in; adherence is calculated from the previous 14 days")
    public CheckInResult create(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateCheckInRequest request) {
        return analyticsService.createCheckIn(principal.id(), request);
    }
}
