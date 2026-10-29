package com.civicconnect.api.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-must-be-at-least-thirty-two-bytes-long";

    @Test
    void parsesTokensSignedWithItsConfiguredSecret() {
        JwtService jwtService = new JwtService(SECRET, 3600);

        String token = jwtService.createAccessToken("citizen@example.com");

        assertThat(jwtService.parseClaims(token).getSubject()).isEqualTo("citizen@example.com");
    }

    @Test
    void rejectsExpiredToken() {
        JwtService jwtService = new JwtService(SECRET, 3600);
        String expiredToken = Jwts.builder()
                .subject("citizen@example.com")
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtService.parseClaims(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rejectsMalformedToken() {
        JwtService jwtService = new JwtService(SECRET, 3600);

        assertThatThrownBy(() -> jwtService.parseClaims("not-a-jwt"))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }
}
