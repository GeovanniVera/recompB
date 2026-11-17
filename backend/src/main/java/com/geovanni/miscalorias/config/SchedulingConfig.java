package com.geovanni.miscalorias.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables the notification sweeps (TMB floor at 20:30, check-in reminder at 08:00).
 * Cron expressions and zone live in application.yml.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
