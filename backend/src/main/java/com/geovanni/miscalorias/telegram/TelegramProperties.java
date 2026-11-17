package com.geovanni.miscalorias.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Telegram delivery configuration.
 *
 * @param botToken      token issued by @BotFather. When blank, delivery is disabled.
 * @param defaultChatId fallback chat id (as text, so an empty env var stays empty)
 *                      used when a user has no chat id linked yet.
 * @param fallbackToLog when true and no chat id is resolvable, the OTP is logged instead
 *                      of failing. Intended for local development only.
 */
@ConfigurationProperties(prefix = "app.telegram")
public record TelegramProperties(
        String botToken,
        String defaultChatId,
        boolean fallbackToLog) {
}
