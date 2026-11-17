package com.geovanni.miscalorias.notifications;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.geovanni.miscalorias.analytics.service.AnalyticsService;
import com.geovanni.miscalorias.auth.domain.AppUser;
import com.geovanni.miscalorias.auth.repository.AppUserRepository;
import com.geovanni.miscalorias.config.AppTimeProperties;
import com.geovanni.miscalorias.notifications.web.dto.TmbCheckResult;
import com.geovanni.miscalorias.profile.service.ProfileService;
import com.geovanni.miscalorias.profile.web.dto.TargetResponse;
import com.geovanni.miscalorias.shared.web.ResourceNotFoundException;
import com.geovanni.miscalorias.telegram.TelegramClient;

/**
 * Two daily sweeps. Both are no-ops when {@code app.notifications.enabled=false}.
 */
@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    static final String CHECK_IN_REMINDER = ("Toca marca quincenal: pésate y mide tu cintura en ayunas, "
            + "y regístralo en Recomp3B.");

    private final AppUserRepository userRepository;
    private final TelegramClient telegramClient;
    private final AnalyticsService analyticsService;
    private final ProfileService profileService;
    private final NotificationProperties properties;
    private final AppTimeProperties timeProperties;

    public NotificationScheduler(AppUserRepository userRepository, TelegramClient telegramClient,
            AnalyticsService analyticsService, ProfileService profileService,
            NotificationProperties properties, AppTimeProperties timeProperties) {
        this.userRepository = userRepository;
        this.telegramClient = telegramClient;
        this.analyticsService = analyticsService;
        this.profileService = profileService;
        this.properties = properties;
        this.timeProperties = timeProperties;
    }

    /** 20:30 local: warn when the day is below the resting metabolic floor. */
    @Scheduled(cron = "${app.notifications.tmb-check-cron:0 30 20 * * *}",
            zone = "${app.time.zone:America/Mexico_City}")
    public void tmbFloorSweep() {
        if (!properties.enabled()) {
            log.debug("Notifications disabled; skipping TMB floor sweep");
            return;
        }
        LocalDate today = LocalDate.now(timeProperties.zoneId());
        for (AppUser user : usersToNotify()) {
            try {
                if (runTmbCheck(user.getId(), today).sent()) {
                    log.info("TMB floor alert sent to user {}", user.getId());
                }
            } catch (RuntimeException ex) {
                // One broken user must not abort the sweep.
                log.error("TMB floor check failed for user {}", user.getId(), ex);
            }
        }
    }

    /** 08:00 local: remind the user when 14+ days passed since the last reference. */
    @Scheduled(cron = "${app.notifications.checkin-reminder-cron:0 0 8 * * *}",
            zone = "${app.time.zone:America/Mexico_City}")
    public void checkInReminderSweep() {
        if (!properties.enabled()) {
            log.debug("Notifications disabled; skipping check-in reminder sweep");
            return;
        }
        LocalDate today = LocalDate.now(timeProperties.zoneId());
        for (AppUser user : usersToNotify()) {
            try {
                if (analyticsService.isCheckInDue(user.getId(), today)) {
                    telegramClient.sendMessage(user.getTelegramChatId(), CHECK_IN_REMINDER);
                    log.info("Check-in reminder sent to user {}", user.getId());
                }
            } catch (RuntimeException ex) {
                log.error("Check-in reminder failed for user {}", user.getId(), ex);
            }
        }
    }

    public TmbCheckResult runTmbCheck(Long userId) {
        return runTmbCheck(userId, LocalDate.now(timeProperties.zoneId()));
    }

    /**
     * Evaluates the TMB floor rule and delivers the alert when it applies.
     * Shared by the scheduled sweep and the manual test endpoint.
     */
    public TmbCheckResult runTmbCheck(Long userId, LocalDate date) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos al usuario %d".formatted(userId)));

        TargetResponse targets = profileService.getTargets(userId);
        long consumedKcal = analyticsService.findDay(userId, date)
                .map(AnalyticsService.DaySnapshot::kcal)
                .orElse(0L);
        long tmb = targets.tmb();
        long missingKcal = Math.max(0L, tmb - consumedKcal);

        if (missingKcal == 0) {
            return new TmbCheckResult(false, consumedKcal, tmb, 0L,
                    "Ya llegaste a tu piso TMB hoy (%d kcal). No hay nada que enviar.".formatted(consumedKcal));
        }

        Long chatId = user.getTelegramChatId();
        if (chatId == null) {
            return new TmbCheckResult(false, consumedKcal, tmb, missingKcal,
                    "Este usuario no tiene un chat de Telegram vinculado.");
        }

        String text = ("Alerta de piso TMB: llevas %d de %d kcal hoy. Te faltan %d kcal para llegar "
                + "a tu mínimo vital (TMB). Come algo con proteína para cuidar el músculo.")
                .formatted(consumedKcal, tmb, missingKcal);
        telegramClient.sendMessage(chatId, text);

        return new TmbCheckResult(true, consumedKcal, tmb, missingKcal, text);
    }

    private List<AppUser> usersToNotify() {
        return userRepository.findByActiveTrueAndTelegramChatIdIsNotNull();
    }
}
