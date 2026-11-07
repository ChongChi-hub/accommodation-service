package vn.toam.identity.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import vn.toam.identity.domain.exception.IdentityDomainException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ProductionRateLimiterSecurityTest {

    @Test
    @DisplayName("In production profile, if Redis is missing/dead, in-memory fallback is strictly FORBIDDEN and throws ERR_RATE_LIMIT_UNAVAILABLE")
    void testProductionFailsWhenRedisUnavailable() {
        Environment env = mock(Environment.class);
        when(env.matchesProfiles("prod", "production")).thenReturn(true);

        RedisRateLimiterAdapter rateLimiter = new RedisRateLimiterAdapter(null, env);

        IdentityDomainException ex1 = assertThrows(IdentityDomainException.class, () ->
                rateLimiter.checkForgotPasswordRateLimit("landlord@toam.vn", "1.2.3.4")
        );
        assertThat(ex1.getErrorCode()).isEqualTo("ERR_RATE_LIMIT_UNAVAILABLE");

        IdentityDomainException ex2 = assertThrows(IdentityDomainException.class, () ->
                rateLimiter.checkPasswordChangeAllowed("u-landlord-01", "1.2.3.4")
        );
        assertThat(ex2.getErrorCode()).isEqualTo("ERR_RATE_LIMIT_UNAVAILABLE");

        IdentityDomainException ex3 = assertThrows(IdentityDomainException.class, () ->
                rateLimiter.recordFailedPasswordChange("u-landlord-01", "1.2.3.4")
        );
        assertThat(ex3.getErrorCode()).isEqualTo("ERR_RATE_LIMIT_UNAVAILABLE");
    }

    @Test
    @DisplayName("In dev profile, in-memory rate limiting is permitted when Redis is absent")
    void testDevPermitsInMemoryFallback() {
        Environment env = mock(Environment.class);
        when(env.matchesProfiles("prod", "production")).thenReturn(false);

        RedisRateLimiterAdapter rateLimiter = new RedisRateLimiterAdapter(null, env);

        // First attempt succeeds in dev
        rateLimiter.checkForgotPasswordRateLimit("dev@toam.vn", "127.0.0.1");

        // Immediate second attempt triggers cooldown in dev
        IdentityDomainException ex = assertThrows(IdentityDomainException.class, () ->
                rateLimiter.checkForgotPasswordRateLimit("dev@toam.vn", "127.0.0.1")
        );
        assertThat(ex.getErrorCode()).isEqualTo("ERR_RATE_LIMIT_COOLDOWN");
    }
}
