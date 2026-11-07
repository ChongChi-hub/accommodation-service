package vn.toam.identity.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.toam.identity.domain.exception.IdentityDomainException;

import static org.junit.jupiter.api.Assertions.*;

class RedisRateLimiterAdapterTest {

    private RedisRateLimiterAdapter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RedisRateLimiterAdapter(null); // in-memory fallback
        rateLimiter.clearAll();
    }

    @Test
    @DisplayName("First request succeeds without error")
    void firstRequestAllowed() {
        assertDoesNotThrow(() -> rateLimiter.checkForgotPasswordRateLimit("user@toam.vn", "1.1.1.1"));
    }

    @Test
    @DisplayName("Second request within 60s fails with cooldown error")
    void cooldownViolationThrows() {
        rateLimiter.checkForgotPasswordRateLimit("user@toam.vn", "1.1.1.1");

        IdentityDomainException ex = assertThrows(IdentityDomainException.class, () ->
                rateLimiter.checkForgotPasswordRateLimit("user@toam.vn", "2.2.2.2"));
        assertEquals("ERR_RATE_LIMIT_COOLDOWN", ex.getErrorCode());
    }

    @Test
    @DisplayName("Same IP requesting for different emails within 60s is blocked by IP cooldown")
    void ipCooldownViolationThrows() {
        rateLimiter.checkForgotPasswordRateLimit("user1@toam.vn", "1.1.1.1");

        IdentityDomainException ex = assertThrows(IdentityDomainException.class, () ->
                rateLimiter.checkForgotPasswordRateLimit("user2@toam.vn", "1.1.1.1"));
        assertEquals("ERR_RATE_LIMIT_COOLDOWN", ex.getErrorCode());
    }
}
