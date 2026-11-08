package vn.cozynest.listing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import vn.cozynest.listing.application.service.UploadRateLimiter;
import vn.cozynest.listing.domain.exception.RateLimitExceededException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("Redis limiter allows uploads within 30 images limit")
    void testRedisRateLimiterWithinLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(10L);

        UploadRateLimiter limiter = new UploadRateLimiter(redisTemplate);
        assertDoesNotThrow(() -> limiter.checkRateLimit(1L, 10));
    }

    @Test
    @DisplayName("Redis limiter throws RateLimitExceededException when exceeding limit")
    void testRedisRateLimiterExceedsLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(35L);

        UploadRateLimiter limiter = new UploadRateLimiter(redisTemplate);
        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class, () ->
                limiter.checkRateLimit(1L, 5));

        assertThat(ex.getErrorCode()).isEqualTo("ERR_UPLOAD_RATE_LIMIT");
    }

    @Test
    @DisplayName("In-memory fallback enforces rate limit when Redis is absent")
    void testInMemoryFallbackRateLimiter() {
        UploadRateLimiter limiter = new UploadRateLimiter(null);
        limiter.clear();

        assertDoesNotThrow(() -> limiter.checkRateLimit(2L, 20));

        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class, () ->
                limiter.checkRateLimit(2L, 15));

        assertThat(ex.getErrorCode()).isEqualTo("ERR_UPLOAD_RATE_LIMIT");
    }
}
