package com.geovanni.miscalorias.auth.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geovanni.miscalorias.auth.domain.AppUser;
import com.geovanni.miscalorias.auth.domain.OtpCode;
import com.geovanni.miscalorias.auth.repository.AppUserRepository;
import com.geovanni.miscalorias.auth.repository.OtpCodeRepository;
import com.geovanni.miscalorias.auth.security.JwtService;
import com.geovanni.miscalorias.auth.security.JwtService.IssuedToken;
import com.geovanni.miscalorias.auth.web.dto.AuthResponse;
import com.geovanni.miscalorias.auth.web.dto.OtpRequestResponse;
import com.geovanni.miscalorias.auth.web.dto.UserResponse;
import com.geovanni.miscalorias.config.OtpProperties;
import com.geovanni.miscalorias.shared.web.InvalidOtpException;
import com.geovanni.miscalorias.shared.web.OtpCooldownException;
import com.geovanni.miscalorias.shared.web.ResourceNotFoundException;
import com.geovanni.miscalorias.telegram.TelegramClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppUserRepository userRepository;
    private final OtpCodeRepository otpRepository;
    private final TelegramClient telegramClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpProperties otpProperties;

    public AuthService(AppUserRepository userRepository,
            OtpCodeRepository otpRepository,
            TelegramClient telegramClient,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            OtpProperties otpProperties) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.telegramClient = telegramClient;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpProperties = otpProperties;
    }

    /**
     * Generates an OTP and delivers it over Telegram. Unknown emails produce the same
     * response as known ones to avoid account enumeration.
     */
    @Transactional
    public OtpRequestResponse requestOtp(String rawEmail) {
        String email = normalize(rawEmail);
        long expiresIn = otpProperties.ttl().toSeconds();
        long resendAfter = otpProperties.resendCooldown().toSeconds();

        AppUser user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !user.isActive()) {
            log.info("OTP requested for an unknown or inactive account: {}", email);
            return new OtpRequestResponse(false, expiresIn, resendAfter,
                    "Si la cuenta existe, enviamos un código.");
        }

        enforceCooldown(user);
        otpRepository.invalidateActiveCodes(user.getId());

        String code = generateCode(otpProperties.codeLength());
        OtpCode otp = new OtpCode(user, passwordEncoder.encode(code), Instant.now().plus(otpProperties.ttl()));
        otpRepository.save(otp);

        // If delivery fails this transaction rolls back, so no dead code stays behind.
        telegramClient.sendOtp(user.getTelegramChatId(), code, otpProperties.ttl());

        return new OtpRequestResponse(true, expiresIn, resendAfter,
                "Si la cuenta existe, enviamos un código.");
    }

    /**
     * Validates the code and issues a JWT. Failed attempts are persisted so brute force
     * is bounded by {@code app.otp.max-attempts}.
     */
    @Transactional(noRollbackFor = InvalidOtpException.class)
    public AuthResponse verifyOtp(String rawEmail, String code) {
        String email = normalize(rawEmail);
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(InvalidOtpException::new);

        OtpCode otp = otpRepository.findFirstByUserIdAndConsumedFalseOrderByIdDesc(user.getId())
                .orElseThrow(InvalidOtpException::new);

        if (otp.isExpired()) {
            throw new InvalidOtpException("El código venció. Pide uno nuevo.");
        }
        if (otp.getAttempts() >= otpProperties.maxAttempts()) {
            throw new InvalidOtpException("Demasiados intentos. Pide un código nuevo.");
        }

        otp.incrementAttempts();
        otpRepository.save(otp);

        if (!passwordEncoder.matches(code, otp.getCodeHash())) {
            throw new InvalidOtpException("El código no es válido.");
        }

        otp.setConsumed(true);
        otpRepository.save(otp);

        IssuedToken issued = jwtService.issue(user);
        return AuthResponse.bearer(issued.value(), issued.expiresInSeconds(), issued.expiresAt(),
                UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos al usuario %d".formatted(userId)));
        return UserResponse.from(user);
    }

    private void enforceCooldown(AppUser user) {
        Duration cooldown = otpProperties.resendCooldown();
        otpRepository.findFirstByUserIdAndConsumedFalseOrderByIdDesc(user.getId()).ifPresent(last -> {
            Instant allowedAt = last.getCreatedAt().plus(cooldown);
            Instant now = Instant.now();
            if (now.isBefore(allowedAt)) {
                throw new OtpCooldownException(Duration.between(now, allowedAt));
            }
        });
    }

    private String generateCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
