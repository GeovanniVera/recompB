package com.geovanni.miscalorias.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.geovanni.miscalorias.auth.domain.AppUser;
import com.geovanni.miscalorias.config.JwtProperties;

import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-long-enough-for-hmac-sha-256";

    private final JwtService jwtService = new JwtService(
            new JwtProperties(SECRET, Duration.ofHours(2), "misCalorias"));

    @Test
    void issuesAndParsesToken() {
        AppUser user = new AppUser("geovanni.vera23@gmail.com", "Geovanni Vera", null);
        ReflectionTestUtils.setField(user, "id", 42L);

        JwtService.IssuedToken issued = jwtService.issue(user);

        assertThat(issued.value()).isNotBlank();
        assertThat(issued.expiresInSeconds()).isEqualTo(7200);

        JwtService.AuthenticatedUser principal = jwtService.parse(issued.value());
        assertThat(principal).isNotNull();
        assertThat(principal.id()).isEqualTo(42L);
        assertThat(principal.email()).isEqualTo("geovanni.vera23@gmail.com");
        assertThat(principal.displayName()).isEqualTo("Geovanni Vera");
    }

    @Test
    void rejectsTamperedToken() {
        AppUser user = new AppUser("a@b.com", "A", null);
        ReflectionTestUtils.setField(user, "id", 1L);

        String token = jwtService.issue(user).value();
        String[] parts = token.split("\\.");
        String tamperedPayload = (parts[1].startsWith("e") ? "f" : "e") + parts[1].substring(1);
        String tampered = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertThat(jwtService.parse(tampered)).isNull();
        assertThat(jwtService.parse("not-a-token")).isNull();
    }

    @Test
    void refusesShortSecretOnStartup() {
        JwtProperties tooShort = new JwtProperties("short", Duration.ofHours(1), "misCalorias");

        assertThatThrownBy(() -> new JwtService(tooShort))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }
}
