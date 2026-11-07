package vn.toam.identity.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
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
    private final Environment environment;

    private static final int COOLDOWN_SECONDS = 60;
    private static final int MAX_HOURLY_REQUESTS = 5;
    private static final int HOURLY_WINDOW_SECONDS = 3600;

    // In-memory fallback structures if Redis is unavailable or in unit tests
    private final Map<String, Instant> inMemoryCooldowns = new ConcurrentHashMap<>();
    private final Map<String, RequestCounter> inMemoryCounters = new ConcurrentHashMap<>();

    public RedisRateLimiterAdapter(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Autowired(required = false) Environment environment) {
        this.redisTemplate = redisTemplate;
        this.environment = environment;
    }

    private boolean isProductionEnvironment() {
        return environment != null && environment.matchesProfiles("prod", "production");
    }

    @Override
    public void checkForgotPasswordRateLimit(String email, String ipAddress) {
        String cleanEmail = email != null ? email.trim().toLowerCase() : "unknown";
        String cleanIp = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress.trim() : "127.0.0.1";

        boolean isProd = isProductionEnvironment();

        try {
            if (redisTemplate != null && isRedisAlive()) {
                checkWithRedis(cleanEmail, cleanIp);
                return;
            }
        } catch (IdentityDomainException e) {
            throw e;
        } catch (Exception ex) {
            if (isProd) {
                log.error("Redis rate limiter failed in production: {}", ex.getMessage());
                throw new IdentityDomainException("Hệ thống kiểm soát tần suất không khả dụng. Vui lòng thử lại sau.", "ERR_RATE_LIMIT_UNAVAILABLE");
            }
            log.warn("Redis unavailable, falling back to in-memory rate limiting: {}", ex.getMessage());
        }

        if (isProd) {
            log.error("Redis not configured/alive in production profile. In-memory fallback is disabled.");
            throw new IdentityDomainException("Hệ thống kiểm soát tần suất không khả dụng trên môi trường production.", "ERR_RATE_LIMIT_UNAVAILABLE");
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

    private static final int MAX_FAILED_PASSWORD_ATTEMPTS = 5;
    private static final int PASSWORD_LOCKOUT_SECONDS = 900;
    private final Map<String, Integer> inMemoryPasswordAttempts = new ConcurrentHashMap<>();
    private final Map<String, Instant> inMemoryPasswordLocks = new ConcurrentHashMap<>();

    @Override
    public void checkPasswordChangeAllowed(String userId, String ipAddress) {
        String cleanUser = userId != null ? userId : "unknown";
        Instant now = Instant.now();
        if (redisTemplate != null && isRedisAlive()) {
            try {
                if (Boolean.TRUE.equals(redisTemplate.hasKey("rate:pwd:lock:user:" + cleanUser))) {
                    throw new IdentityDomainException("Ban da nhap sai mat khau qua 5 lan. Vui long thu lai sau 15 phut.", "ERR_AUTH_PASSWORD_CHANGE_LOCKED");
                }
                return;
            } catch (IdentityDomainException e) { throw e; } catch (Exception ignored) {}
        }
        Instant lockedUntil = inMemoryPasswordLocks.get(cleanUser);
        if (lockedUntil != null && now.isBefore(lockedUntil)) {
            throw new IdentityDomainException("Ban da nhap sai mat khau qua 5 lan. Vui long thu lai sau 15 phut.", "ERR_AUTH_PASSWORD_CHANGE_LOCKED");
        }
    }

    @Override
    public void recordFailedPasswordChange(String userId, String ipAddress) {
        String cleanUser = userId != null ? userId : "unknown";
        Instant now = Instant.now();
        if (redisTemplate != null && isRedisAlive()) {
            try {
                Long attempts = redisTemplate.opsForValue().increment("rate:pwd:cnt:user:" + cleanUser);
                if (attempts != null && attempts == 1) redisTemplate.expire("rate:pwd:cnt:user:" + cleanUser, Duration.ofSeconds(PASSWORD_LOCKOUT_SECONDS));
                if (attempts != null && attempts >= MAX_FAILED_PASSWORD_ATTEMPTS) {
                    redisTemplate.opsForValue().set("rate:pwd:lock:user:" + cleanUser, "LOCKED", Duration.ofSeconds(PASSWORD_LOCKOUT_SECONDS));
                    redisTemplate.delete("rate:pwd:cnt:user:" + cleanUser);
                }
                return;
            } catch (Exception ignored) {}
        }
        int attempts = inMemoryPasswordAttempts.getOrDefault(cleanUser, 0) + 1;
        if (attempts >= MAX_FAILED_PASSWORD_ATTEMPTS) {
            inMemoryPasswordLocks.put(cleanUser, now.plusSeconds(PASSWORD_LOCKOUT_SECONDS));
            inMemoryPasswordAttempts.remove(cleanUser);
        } else {
            inMemoryPasswordAttempts.put(cleanUser, attempts);
        }
    }

    @Override
    public void clearPasswordChangeAttempts(String userId, String ipAddress) {
        String cleanUser = userId != null ? userId : "unknown";
        if (redisTemplate != null && isRedisAlive()) {
            try {
                redisTemplate.delete("rate:pwd:cnt:user:" + cleanUser);
                redisTemplate.delete("rate:pwd:lock:user:" + cleanUser);
            } catch (Exception ignored) {}
        }
        inMemoryPasswordAttempts.remove(cleanUser);
        inMemoryPasswordLocks.remove(cleanUser);
    }

    public void clearAll() {
        inMemoryCooldowns.clear();
        inMemoryCounters.clear();
        inMemoryPasswordAttempts.clear();
        inMemoryPasswordLocks.clear();
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

