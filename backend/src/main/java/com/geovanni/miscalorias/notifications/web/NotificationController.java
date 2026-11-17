package com.geovanni.miscalorias.notifications.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geovanni.miscalorias.auth.security.JwtService.AuthenticatedUser;
import com.geovanni.miscalorias.notifications.NotificationScheduler;
import com.geovanni.miscalorias.notifications.web.dto.TmbCheckResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Manual triggers to verify Telegram rules without waiting for cron")
public class NotificationController {

    private final NotificationScheduler notificationScheduler;

    public NotificationController(NotificationScheduler notificationScheduler) {
        this.notificationScheduler = notificationScheduler;
    }

    @PostMapping("/test-tmb-check")
    @Operation(summary = "Run the TMB floor rule now and send the alert if it applies")
    public TmbCheckResult testTmbCheck(@AuthenticationPrincipal AuthenticatedUser principal) {
        return notificationScheduler.runTmbCheck(principal.id());
    }
}
