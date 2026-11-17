package com.geovanni.miscalorias.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.geovanni.miscalorias.auth.domain.AppUser;
import com.geovanni.miscalorias.auth.domain.OtpCode;
import com.geovanni.miscalorias.auth.repository.AppUserRepository;
import com.geovanni.miscalorias.auth.repository.OtpCodeRepository;
import com.geovanni.miscalorias.auth.security.JwtService;
import com.geovanni.miscalorias.auth.web.dto.AuthResponse;
import com.geovanni.miscalorias.auth.web.dto.OtpRequestResponse;
import com.geovanni.miscalorias.config.JwtProperties;
import com.geovanni.miscalorias.config.OtpProperties;
import com.geovanni.miscalorias.shared.web.InvalidOtpException;
import com.geovanni.miscalorias.shared.web.OtpCooldownException;
import com.geovanni.miscalorias.telegram.TelegramClient;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final OtpProperties OTP_PROPERTIES =
            new OtpProperties(Duration.ofMinutes(5), 5, Duration.ofSeconds(30), 6);

    @Mock
    private AppUserRepository userRepository;
    @Mock
    private OtpCodeRepository otpRepository;
    @Mock
    private TelegramClient telegramClient;

    private AuthService authService;
    private AppUser user;

    @BeforeEach
    void setUp() {
        PasswordEncoder encoder = new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return "h:" + rawPassword;
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return encodedPassword.equals("h:" + rawPassword);
            }
        };
        JwtService jwtService = new JwtService(
                new JwtProperties("test-secret-that-is-long-enough-for-hmac-sha-256",
                        Duration.ofHours(1), "misCalorias"));
        authService = new AuthService(userRepository, otpRepository, telegramClient, encoder, jwtService,
                OTP_PROPERTIES);
        user = new AppUser("geovanni.vera23@gmail.com", "Geovanni Vera", 1234L);
        ReflectionTestUtils.setField(user, "id", 7L);
    }

    @Test
    void unknownEmailDoesNotLeakAndSendsNothing() {
        when(userRepository.findByEmailIgnoreCase("nobody@test.com")).thenReturn(Optional.empty());

        OtpRequestResponse response = authService.requestOtp(" Nobody@Test.com ");

        assertThat(response.sent()).isFalse();
        verify(telegramClient, never()).sendOtp(any(), anyString(), any());
    }

    @Test
    void issuesSixDigitCodeAndDeliversIt() {
        when(userRepository.findByEmailIgnoreCase("geovanni.vera23@gmail.com")).thenReturn(Optional.of(user));
        when(otpRepository.findFirstByUserIdAndConsumedFalseOrderByIdDesc(7L)).thenReturn(Optional.empty());
        when(otpRepository.save(any(OtpCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OtpRequestResponse response = authService.requestOtp("geovanni.vera23@gmail.com");

        assertThat(response.sent()).isTrue();
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(telegramClient).sendOtp(eq(1234L), code.capture(), eq(Duration.ofMinutes(5)));
        assertThat(code.getValue()).matches("\\d{6}");
        verify(otpRepository).invalidateActiveCodes(7L);
    }

    @Test
    void blocksResendDuringCooldown() {
        when(userRepository.findByEmailIgnoreCase("geovanni.vera23@gmail.com")).thenReturn(Optional.of(user));
        OtpCode recent = new OtpCode(user, "h:123456", Instant.now().plus(Duration.ofMinutes(5)));
        ReflectionTestUtils.setField(recent, "createdAt", Instant.now().minusSeconds(5));
        when(otpRepository.findFirstByUserIdAndConsumedFalseOrderByIdDesc(7L)).thenReturn(Optional.of(recent));

        assertThatThrownBy(() -> authService.requestOtp("geovanni.vera23@gmail.com"))
                .isInstanceOf(OtpCooldownException.class);
        verify(telegramClient, never()).sendOtp(any(), anyString(), any());
    }

    @Test
    void validCodeReturnsToken() {
        OtpCode otp = new OtpCode(user, "h:123456", Instant.now().plus(Duration.ofMinutes(5)));
        when(userRepository.findByEmailIgnoreCase("geovanni.vera23@gmail.com")).thenReturn(Optional.of(user));
        when(otpRepository.findFirstByUserIdAndConsumedFalseOrderByIdDesc(7L)).thenReturn(Optional.of(otp));
        when(otpRepository.save(any(OtpCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.verifyOtp("geovanni.vera23@gmail.com", "123456");

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.user().email()).isEqualTo("geovanni.vera23@gmail.com");
        assertThat(otp.isConsumed()).isTrue();
    }

    @Test
    void wrongCodeCountsAnAttemptAndFails() {
        OtpCode otp = new OtpCode(user, "h:123456", Instant.now().plus(Duration.ofMinutes(5)));
        when(userRepository.findByEmailIgnoreCase("geovanni.vera23@gmail.com")).thenReturn(Optional.of(user));
        when(otpRepository.findFirstByUserIdAndConsumedFalseOrderByIdDesc(7L)).thenReturn(Optional.of(otp));
        when(otpRepository.save(any(OtpCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> authService.verifyOtp("geovanni.vera23@gmail.com", "000000"))
                .isInstanceOf(InvalidOtpException.class);
        assertThat(otp.getAttempts()).isEqualTo(1);
        assertThat(otp.isConsumed()).isFalse();
    }

    @Test
    void expiredCodeIsRejected() {
        OtpCode otp = new OtpCode(user, "h:123456", Instant.now().minusSeconds(1));
        when(userRepository.findByEmailIgnoreCase("geovanni.vera23@gmail.com")).thenReturn(Optional.of(user));
        when(otpRepository.findFirstByUserIdAndConsumedFalseOrderByIdDesc(7L)).thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> authService.verifyOtp("geovanni.vera23@gmail.com", "123456"))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("venció");
    }

    @Test
    void exhaustedAttemptsAreRejected() {
        OtpCode otp = new OtpCode(user, "h:123456", Instant.now().plus(Duration.ofMinutes(5)));
        for (int i = 0; i < 5; i++) {
            otp.incrementAttempts();
        }
        when(userRepository.findByEmailIgnoreCase("geovanni.vera23@gmail.com")).thenReturn(Optional.of(user));
        when(otpRepository.findFirstByUserIdAndConsumedFalseOrderByIdDesc(7L)).thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> authService.verifyOtp("geovanni.vera23@gmail.com", "123456"))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("Demasiados intentos");
    }
}
