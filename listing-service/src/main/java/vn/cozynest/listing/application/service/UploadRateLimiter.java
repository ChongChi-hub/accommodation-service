package vn.cozynest.listing.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import vn.cozynest.listing.domain.exception.RateLimitExceededException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Component
public class UploadRateLimiter {

    public static final int MAX_UPLOADS_PER_MINUTE = 30;
    private final StringRedisTemplate redisTemplate;
    private final Map<Long, Queue<Instant>> fallbackMemoryLimits = new ConcurrentHashMap<>();

    public UploadRateLimiter(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void clear() {
        fallbackMemoryLimits.clear();
    }

    public void checkRateLimit(Long landlordId, int incomingCount) {
        if (incomingCount <= 0) return;

        if (redisTemplate != null) {
            try {
                String key = "rate_limit:images:landlord:" + landlordId;
                Long count = redisTemplate.opsForValue().increment(key, incomingCount);
                if (count != null && count == incomingCount) {
                    redisTemplate.expire(key, Duration.ofSeconds(60));
                }
                if (count != null && count > MAX_UPLOADS_PER_MINUTE) {
                    throw new RateLimitExceededException(
                            "Bạn đã vượt quá giới hạn tải ảnh lên (tối đa " + MAX_UPLOADS_PER_MINUTE + " ảnh/phút). Vui lòng thử lại sau.",
                            "ERR_UPLOAD_RATE_LIMIT"
                    );
                }
                return;
            } catch (RateLimitExceededException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Redis rate limiter unavailable, falling back to memory: {}", e.getMessage());
            }
        }

        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(60);
        Queue<Instant> timestamps = fallbackMemoryLimits.computeIfAbsent(landlordId, k -> new ConcurrentLinkedQueue<>());

        while (!timestamps.isEmpty() && timestamps.peek().isBefore(cutoff)) {
            timestamps.poll();
        }

        if (timestamps.size() + incomingCount > MAX_UPLOADS_PER_MINUTE) {
            throw new RateLimitExceededException(
                    "Bạn đã vượt quá giới hạn tải ảnh lên (tối đa " + MAX_UPLOADS_PER_MINUTE + " ảnh/phút). Vui lòng thử lại sau.",
                    "ERR_UPLOAD_RATE_LIMIT"
            );
        }
        for (int i = 0; i < incomingCount; i++) {
            timestamps.add(now);
        }
    }
}
