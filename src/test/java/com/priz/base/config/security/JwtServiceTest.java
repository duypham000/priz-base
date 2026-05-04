package com.priz.base.config.security;

import com.priz.common.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "test-secret-key-must-be-at-least-32-characters-long-for-hmac";
    private static final long ACCESS_TOKEN_EXPIRATION = 3600000L;
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);
        jwtService.init();
    }

    @Test
    void generateAccessToken_should_createValidToken() {
        String token = jwtService.generateAccessToken("user-1", "test@example.com", "testuser", "USER");

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void getUserIdFromToken_should_returnSubject() {
        String token = jwtService.generateAccessToken("user-1", "test@example.com", "testuser", "USER");

        assertEquals("user-1", jwtService.getUserIdFromToken(token));
    }

    @Test
    void getEmailFromToken_should_returnEmail() {
        String token = jwtService.generateAccessToken("user-1", "test@example.com", "testuser", "USER");

        assertEquals("test@example.com", jwtService.getEmailFromToken(token));
    }

    @Test
    void getUsernameFromToken_should_returnUsername() {
        String token = jwtService.generateAccessToken("user-1", "test@example.com", "testuser", "USER");

        assertEquals("testuser", jwtService.getUsernameFromToken(token));
    }

    @Test
    void getRoleFromToken_should_returnRole() {
        String token = jwtService.generateAccessToken("user-1", "test@example.com", "testuser", "USER");

        assertEquals("USER", jwtService.getRoleFromToken(token));
    }

    @Test
    void validateToken_should_returnFalse_forMalformedToken() {
        assertFalse(jwtService.validateToken("not-a-valid-jwt-token"));
    }

    @Test
    void validateToken_should_returnFalse_forEmptyToken() {
        assertFalse(jwtService.validateToken(""));
    }

    @Test
    void validateToken_should_returnFalse_forExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 0L);
        jwtService.init();

        String token = jwtService.generateAccessToken("user-1", "test@example.com", "testuser", "USER");

        assertFalse(jwtService.validateToken(token));
    }

    @Test
    void generateRefreshTokenValue_should_returnUuidFormat() {
        String refreshToken = jwtService.generateRefreshTokenValue();

        assertNotNull(refreshToken);
        assertDoesNotThrow(() -> java.util.UUID.fromString(refreshToken));
    }

    @Test
    void getRefreshTokenExpirationMs_should_returnConfiguredValue() {
        assertEquals(REFRESH_TOKEN_EXPIRATION, jwtService.getRefreshTokenExpirationMs());
    }
}
