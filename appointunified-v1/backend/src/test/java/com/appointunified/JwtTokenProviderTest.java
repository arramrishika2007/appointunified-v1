package com.appointunified;

import com.appointunified.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        // Base64 encoded 64-char string for HS512
        ReflectionTestUtils.setField(provider, "jwtSecret",
            "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW9ubHktbXVzdC1iZS1sb25nLWVub3VnaA==");
        ReflectionTestUtils.setField(provider, "accessTokenExpiryMs",  900_000L);
        ReflectionTestUtils.setField(provider, "refreshTokenExpiryMs", 2_592_000_000L);
    }

    @Test
    void generateAccessToken_isValid() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateAccessToken(userId, "PUBLIC");

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUserIdFromToken(token)).isEqualTo(userId);
        assertThat(provider.getRoleFromToken(token)).isEqualTo("PUBLIC");
    }

    @Test
    void generateRefreshToken_isValid() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateRefreshToken(userId);

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUserIdFromToken(token)).isEqualTo(userId);
    }

    @Test
    void validateToken_withGarbage_returnsFalse() {
        assertThat(provider.validateToken("not.a.jwt")).isFalse();
        assertThat(provider.validateToken("")).isFalse();
        assertThat(provider.validateToken(null)).isFalse();
    }

    @Test
    void validateToken_withTamperedToken_returnsFalse() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateAccessToken(userId, "PUBLIC");
        // Tamper with the signature
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertThat(provider.validateToken(tampered)).isFalse();
    }

    @Test
    void getRefreshTokenExpiryMs_returnsConfiguredValue() {
        assertThat(provider.getRefreshTokenExpiryMs()).isEqualTo(2_592_000_000L);
    }
}
