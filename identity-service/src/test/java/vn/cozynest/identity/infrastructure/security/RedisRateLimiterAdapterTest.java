package vn.cozynest.identity.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import vn.cozynest.identity.domain.exception.IdentityDomainException;

import static org.junit.jupiter.api.Assertions.*;

class RedisRateLimiterAdapterTest {

    private RedisRateLimiterAdapter rateLimiter;

    @BeforeEach
    void setUp() {
        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("dev");
        rateLimiter = new RedisRateLimiterAdapter(null, devEnv); // in-memory fallback allowed in dev
        rateLimiter.clearAll();
    }

    @Test
    @DisplayName("First request succeeds without error")
    void firstRequestAllowed() {
        assertDoesNotThrow(() -> rateLimiter.checkForgotPasswordRateLimit("user@cozynest.vn", "1.1.1.1"));
    }

    @Test
    @DisplayName("Second request within 60s fails with cooldown error")
    void cooldownViolationThrows() {
        rateLimiter.checkForgotPasswordRateLimit("user@cozynest.vn", "1.1.1.1");

        IdentityDomainException ex = assertThrows(IdentityDomainException.class, () ->
                rateLimiter.checkForgotPasswordRateLimit("user@cozynest.vn", "2.2.2.2"));
        assertEquals("ERR_RATE_LIMIT_COOLDOWN", ex.getErrorCode());
    }

    @Test
    @DisplayName("Same IP requesting for different emails within 60s is blocked by IP cooldown")
    void ipCooldownViolationThrows() {
        rateLimiter.checkForgotPasswordRateLimit("user1@cozynest.vn", "1.1.1.1");

        IdentityDomainException ex = assertThrows(IdentityDomainException.class, () ->
                rateLimiter.checkForgotPasswordRateLimit("user2@cozynest.vn", "1.1.1.1"));
        assertEquals("ERR_RATE_LIMIT_COOLDOWN", ex.getErrorCode());
    }

    @Test
    @DisplayName("In production profile, in-memory fallback is disabled and throws exception when Redis is missing")
    void productionProfileDisablesInMemoryFallback() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("production");

        RedisRateLimiterAdapter prodRateLimiter = new RedisRateLimiterAdapter(null, prodEnv);

        IdentityDomainException ex = assertThrows(IdentityDomainException.class, () ->
                prodRateLimiter.checkForgotPasswordRateLimit("user@cozynest.vn", "1.2.3.4"));
        assertEquals("ERR_RATE_LIMIT_UNAVAILABLE", ex.getErrorCode());
    }
}

