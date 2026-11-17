package com.geovanni.miscalorias.shared.web;

import org.springframework.http.HttpStatus;

public class InvalidOtpException extends ApiException {

    public InvalidOtpException() {
        this("El código no es válido o ya venció.");
    }

    public InvalidOtpException(String message) {
        super(HttpStatus.UNAUTHORIZED, "invalid_otp", message);
    }
}
