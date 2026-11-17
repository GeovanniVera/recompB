package com.geovanni.miscalorias.config;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Single timezone used by scheduled jobs and "today" defaults, so a UTC container
 * still reasons in the user's local day.
 */
@ConfigurationProperties(prefix = "app.time")
public record AppTimeProperties(String zone) {

    public static final String DEFAULT_ZONE = "America/Mexico_City";

    public ZoneId zoneId() {
        return ZoneId.of(zone == null || zone.isBlank() ? DEFAULT_ZONE : zone);
    }
}
