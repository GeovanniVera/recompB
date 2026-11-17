package com.geovanni.miscalorias.auth.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geovanni.miscalorias.auth.security.JwtService.AuthenticatedUser;
import com.geovanni.miscalorias.auth.service.AuthService;
import com.geovanni.miscalorias.auth.web.dto.AuthResponse;
import com.geovanni.miscalorias.auth.web.dto.OtpRequest;
import com.geovanni.miscalorias.auth.web.dto.OtpRequestResponse;
import com.geovanni.miscalorias.auth.web.dto.OtpVerifyRequest;
import com.geovanni.miscalorias.auth.web.dto.UserResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Passwordless authentication using Telegram-delivered OTP codes")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/otp/request")
    @Operation(summary = "Request a one-time code", description = "Generates an OTP and delivers it over Telegram.")
    public ResponseEntity<OtpRequestResponse> requestOtp(@Valid @RequestBody OtpRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(authService.requestOtp(request.email()));
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Verify a code and obtain a JWT")
    public AuthResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return authService.verifyOtp(request.email(), request.code());
    }

    @GetMapping("/me")
    @Operation(summary = "Current authenticated user")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return authService.currentUser(principal.id());
    }
}
