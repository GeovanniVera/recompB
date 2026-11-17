package com.geovanni.miscalorias.shared.web;

import org.springframework.http.HttpStatus;

public class TelegramDeliveryException extends ApiException {

    public TelegramDeliveryException(String message) {
        super(HttpStatus.BAD_GATEWAY, "telegram_delivery_failed", message);
    }
}
