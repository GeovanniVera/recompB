package com.geovanni.miscalorias.shared.web;

import org.springframework.http.HttpStatus;

public class TelegramNotConfiguredException extends ApiException {

    public TelegramNotConfiguredException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "telegram_not_configured", message);
    }
}
