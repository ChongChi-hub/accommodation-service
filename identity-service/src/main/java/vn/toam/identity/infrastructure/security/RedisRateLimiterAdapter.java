package vn.toam.identity.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import vn.toam.identity.application.port.RateLimiterPort;
import vn.toam.identity.domain.exception.IdentityDomainException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RedisRateLimiterAdapter implements RateLimiterPort {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiterAdapter.class);

    private final StringRedisTemplate redisTemplate;

    private static final int COOLDOWN_SECONDS = 60;
    private static final int MAX_HOURLY_REQUESTS = 5;
    private static final int HOURLY_WINDOW_SECONDS = 3600;

    // In-memory fallback structures if Redis is unavailable or in unit tests
    private final Map<String, Instant> inMemoryCooldowns = new ConcurrentHashMap<>();
    private final Map<String, RequestCounter> inMemoryCounters = new ConcurrentHashMap<>();

    public RedisRateLimiterAdapter(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void checkForgotPasswordRateLimit(String email, String ipAddress) {
        String cleanEmail = email != null ? email.trim().toLowerCase() : "unknown";
        String cleanIp = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress.trim() : "127.0.0.1";

        try {
            if (redisTemplate != null && isRedisAlive()) {
                checkWithRedis(cleanEmail, cleanIp);
                return;
            }
        } catch (Exception ex) {
            log.warn("Redis unavailable, falling back to in-memory rate limiting: {}", ex.getMessage());
        }

        checkWithInMemory(cleanEmail, cleanIp);
    }

    private boolean isRedisAlive() {
        try {
            return redisTemplate.getConnectionFactory() != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void checkWithRedis(String email, String ip) {
        String cdEmailKey = "rate:forgot:cd:email:" + email;
        String cdIpKey = "rate:forgot:cd:ip:" + ip;
        String countEmailKey = "rate:forgot:cnt:email:" + email;
        String countIpKey = "rate:forgot:cnt:ip:" + ip;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(cdEmailKey)) ||
            Boolean.TRUE.equals(redisTemplate.hasKey(cdIpKey))) {
            throw new IdentityDomainException("Vui lòng đợi ít nhất 60 giây trước khi yêu cầu lại mã OTP.", "ERR_RATE_LIMIT_COOLDOWN");
        }

        String emailCountStr = redisTemplate.opsForValue().get(countEmailKey);
        if (emailCountStr != null && Integer.parseInt(emailCountStr) >= MAX_HOURLY_REQUESTS) {
            throw new IdentityDomainException("Bạn đã vượt quá số lần yêu cầu mã OTP (tối đa 5 lần/giờ). Vui lòng thử lại sau.", "ERR_RATE_LIMIT_EXCEEDED");
        }

        String ipCountStr = redisTemplate.opsForValue().get(countIpKey);
        if (ipCountStr != null && Integer.parseInt(ipCountStr) >= MAX_HOURLY_REQUESTS) {
            throw new IdentityDomainException("Địa chỉ IP của bạn đã vượt quá giới hạn yêu cầu OTP (tối đa 5 lần/giờ).", "ERR_RATE_LIMIT_EXCEEDED");
        }

        redisTemplate.opsForValue().set(cdEmailKey, "1", Duration.ofSeconds(COOLDOWN_SECONDS));
        redisTemplate.opsForValue().set(cdIpKey, "1", Duration.ofSeconds(COOLDOWN_SECONDS));

        Long emailCount = redisTemplate.opsForValue().increment(countEmailKey);
        if (emailCount != null && emailCount == 1) {
            redisTemplate.expire(countEmailKey, Duration.ofSeconds(HOURLY_WINDOW_SECONDS));
        }

        Long ipCount = redisTemplate.opsForValue().increment(countIpKey);
        if (ipCount != null && ipCount == 1) {
            redisTemplate.expire(countIpKey, Duration.ofSeconds(HOURLY_WINDOW_SECONDS));
        }
    }

    private synchronized void checkWithInMemory(String email, String ip) {
        Instant now = Instant.now();

        // 1. Check cooldown
        Instant emailCd = inMemoryCooldowns.get(email);
        if (emailCd != null && now.isBefore(emailCd)) {
            throw new IdentityDomainException("Vui lòng đợi ít nhất 60 giây trước khi yêu cầu lại mã OTP.", "ERR_RATE_LIMIT_COOLDOWN");
        }

        Instant ipCd = inMemoryCooldowns.get(ip);
        if (ipCd != null && now.isBefore(ipCd)) {
            throw new IdentityDomainException("Vui lòng đợi ít nhất 60 giây trước khi yêu cầu lại mã OTP.", "ERR_RATE_LIMIT_COOLDOWN");
        }

        // 2. Check hourly count
        RequestCounter emailCounter = inMemoryCounters.computeIfAbsent(email, k -> new RequestCounter(now));
        if (emailCounter.isExpired(now)) {
            emailCounter.reset(now);
        }
        if (emailCounter.count >= MAX_HOURLY_REQUESTS) {
            throw new IdentityDomainException("Bạn đã vượt quá số lần yêu cầu mã OTP (tối đa 5 lần/giờ). Vui lòng thử lại sau.", "ERR_RATE_LIMIT_EXCEEDED");
        }

        RequestCounter ipCounter = inMemoryCounters.computeIfAbsent(ip, k -> new RequestCounter(now));
        if (ipCounter.isExpired(now)) {
            ipCounter.reset(now);
        }
        if (ipCounter.count >= MAX_HOURLY_REQUESTS) {
            throw new IdentityDomainException("Địa chỉ IP của bạn đã vượt quá giới hạn yêu cầu OTP (tối đa 5 lần/giờ).", "ERR_RATE_LIMIT_EXCEEDED");
        }

        // Record usage
        inMemoryCooldowns.put(email, now.plusSeconds(COOLDOWN_SECONDS));
        inMemoryCooldowns.put(ip, now.plusSeconds(COOLDOWN_SECONDS));
        emailCounter.count++;
        ipCounter.count++;
    }

    public void clearAll() {
        inMemoryCooldowns.clear();
        inMemoryCounters.clear();
    }

    private static class RequestCounter {
        int count;
        Instant windowStart;

        RequestCounter(Instant now) {
            this.count = 0;
            this.windowStart = now;
        }

        boolean isExpired(Instant now) {
            return Duration.between(windowStart, now).getSeconds() > HOURLY_WINDOW_SECONDS;
        }

        void reset(Instant now) {
            this.count = 0;
            this.windowStart = now;
        }
    }
}
