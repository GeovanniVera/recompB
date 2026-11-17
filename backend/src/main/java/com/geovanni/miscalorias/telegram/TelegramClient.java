package com.geovanni.miscalorias.telegram;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.geovanni.miscalorias.shared.web.TelegramDeliveryException;
import com.geovanni.miscalorias.shared.web.TelegramNotConfiguredException;

/**
 * Minimal Telegram Bot API client used for OTP codes and notifications.
 */
@Service
public class TelegramClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramClient.class);
    private static final String API_BASE_URL = "https://api.telegram.org";

    private final TelegramProperties properties;
    private final RestClient restClient;

    public TelegramClient(TelegramProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.baseUrl(API_BASE_URL).build();
    }

    /**
     * Sends the OTP to the given chat, falling back to the configured default chat.
     */
    public void sendOtp(Long userChatId, String code, Duration ttl) {
        String text = "Tu código de acceso a Recomp3B es: <b>%s</b>%nVence en %d minutos."
                .formatted(code, Math.max(1, ttl.toMinutes()));
        sendMessage(userChatId, text);
    }

    /**
     * Sends an arbitrary HTML message.
     *
     * @throws TelegramNotConfiguredException when no token/chat is available and the
     *                                        log fallback is disabled
     * @throws TelegramDeliveryException      when the Telegram API rejects the message
     */
    public void sendMessage(Long userChatId, String text) {
        Long chatId = userChatId != null ? userChatId : parseDefaultChatId();
        if (!StringUtils.hasText(properties.botToken()) || chatId == null) {
            if (properties.fallbackToLog()) {
                log.warn("Telegram is not configured. Message for chat {}: {}", chatId, text);
                return;
            }
            throw new TelegramNotConfiguredException(
                    "El envío por Telegram no está configurado. Configura TELEGRAM_BOT_TOKEN y TELEGRAM_CHAT_ID.");
        }

        try {
            restClient.post()
                    .uri("/bot{token}/sendMessage", properties.botToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SendMessageRequest(chatId, text, "HTML"))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Telegram message delivered to chat {}", chatId);
        } catch (RestClientException ex) {
            // Never log the bot token: the exception message may contain the request URI.
            log.error("Telegram delivery to chat {} failed: {}", chatId, ex.getClass().getSimpleName());
            throw new TelegramDeliveryException("No pudimos enviar el mensaje por Telegram. Intenta de nuevo más tarde.");
        }
    }

    private Long parseDefaultChatId() {
        String value = properties.defaultChatId();
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            log.warn("Ignoring invalid app.telegram.default-chat-id value: {}", value);
            return null;
        }
    }

    private record SendMessageRequest(Long chat_id, String text, String parse_mode) {
    }
}
