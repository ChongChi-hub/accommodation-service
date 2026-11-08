package vn.cozynest.identity.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import vn.cozynest.identity.application.port.RateLimiterPort;
import vn.cozynest.identity.domain.exception.IdentityDomainException;

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
    private static final int MAX_FAILED_PASSWORD_ATTEMPTS = 5;
    private static final int PASSWORD_LOCKOUT_SECONDS = 900;
    private static final int MAX_LOGIN_PER_5M = 30;
    private static final int MAX_REGISTER_PER_HOUR = 10;

    private final Map<String, Instant> inMemoryCooldowns = new ConcurrentHashMap<>();
    private final Map<String, Integer> inMemoryCounters = new ConcurrentHashMap<>();
    private final Map<String, Integer> inMemoryPasswordAttempts = new ConcurrentHashMap<>();
    private final Map<String, Instant> inMemoryPasswordLocks = new ConcurrentHashMap<>();

    public RedisRateLimiterAdapter(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Autowired(required = false) Environment environment) {
        this.redisTemplate = redisTemplate;
        this.environment = environment;
    }

    private boolean isProd() {
        return environment != null && environment.matchesProfiles("prod", "production");
    }

    private boolean isRedisAlive() {
        try {
            return redisTemplate != null && redisTemplate.getConnectionFactory() != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void handleProdFailClosed(String op, Exception ex) {
        if (isProd()) {
            log.error("Redis rate limiter failed in production for {}: {}", op, ex.getMessage());
            throw new IdentityDomainException("Hệ thống kiểm soát tần suất không khả dụng trên môi trường production.", "ERR_RATE_LIMIT_UNAVAILABLE");
        }
        log.warn("Redis unavailable, using in-memory rate limiter for {}: {}", op, ex.getMessage());
    }

    @Override
    public void checkLoginRateLimit(String ipAddress) {
        String cleanIp = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress.trim() : "127.0.0.1";
        String key = "rate:login:ip:" + cleanIp;
        checkSlidingLimit(key, cleanIp, MAX_LOGIN_PER_5M, 300, "ERR_LOGIN_RATE_LIMIT_EXCEEDED", "Bạn đã thử đăng nhập quá nhiều lần. Vui lòng thử lại sau 5 phút.");
    }

    @Override
    public void checkRegisterRateLimit(String ipAddress) {
        String cleanIp = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress.trim() : "127.0.0.1";
        String key = "rate:register:ip:" + cleanIp;
        checkSlidingLimit(key, cleanIp, MAX_REGISTER_PER_HOUR, 3600, "ERR_REGISTER_RATE_LIMIT_EXCEEDED", "Bạn đã đăng ký quá nhiều tài khoản từ IP này. Vui lòng thử lại sau 1 giờ.");
    }

    private void checkSlidingLimit(String redisKey, String inMemKey, int maxReqs, int windowSecs, String errCode, String errMsg) {
        try {
            if (isRedisAlive()) {
                Long count = redisTemplate.opsForValue().increment(redisKey);
                if (count != null && count == 1) {
                    redisTemplate.expire(redisKey, Duration.ofSeconds(windowSecs));
                }
                if (count != null && count > maxReqs) {
                    throw new IdentityDomainException(errMsg, errCode);
                }
                return;
            }
        } catch (IdentityDomainException e) {
            throw e;
        } catch (Exception ex) {
            handleProdFailClosed(redisKey, ex);
        }

        if (isProd()) {
            throw new IdentityDomainException("Hệ thống kiểm soát tần suất không khả dụng trên môi trường production.", "ERR_RATE_LIMIT_UNAVAILABLE");
        }

        int count = inMemoryCounters.merge(inMemKey, 1, Integer::sum);
        if (count > maxReqs) {
            throw new IdentityDomainException(errMsg, errCode);
        }
    }

    @Override
    public void checkForgotPasswordRateLimit(String email, String ipAddress) {
        String cleanEmail = email != null ? email.trim().toLowerCase() : "unknown";
        String cleanIp = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress.trim() : "127.0.0.1";

        try {
            if (isRedisAlive()) {
                String cdEmailKey = "rate:forgot:cd:email:" + cleanEmail;
                String cdIpKey = "rate:forgot:cd:ip:" + cleanIp;
                String cntEmailKey = "rate:forgot:cnt:email:" + cleanEmail;
                String cntIpKey = "rate:forgot:cnt:ip:" + cleanIp;

                if (Boolean.TRUE.equals(redisTemplate.hasKey(cdEmailKey)) || Boolean.TRUE.equals(redisTemplate.hasKey(cdIpKey))) {
                    throw new IdentityDomainException("Vui lòng đợi ít nhất 60 giây trước khi yêu cầu lại mã OTP.", "ERR_RATE_LIMIT_COOLDOWN");
                }
                checkCount(cntEmailKey, MAX_HOURLY_REQUESTS, "Bạn đã vượt quá số lần yêu cầu mã OTP (tối đa 5 lần/giờ). Vui lòng thử lại sau.");
                checkCount(cntIpKey, MAX_HOURLY_REQUESTS, "Địa chỉ IP của bạn đã vượt quá giới hạn yêu cầu OTP (tối đa 5 lần/giờ).");

                redisTemplate.opsForValue().set(cdEmailKey, "1", Duration.ofSeconds(COOLDOWN_SECONDS));
                redisTemplate.opsForValue().set(cdIpKey, "1", Duration.ofSeconds(COOLDOWN_SECONDS));
                incrementWithExpire(cntEmailKey, HOURLY_WINDOW_SECONDS);
                incrementWithExpire(cntIpKey, HOURLY_WINDOW_SECONDS);
                return;
            }
        } catch (IdentityDomainException e) {
            throw e;
        } catch (Exception ex) {
            handleProdFailClosed("forgot_password", ex);
        }

        if (isProd()) {
            throw new IdentityDomainException("Hệ thống kiểm soát tần suất không khả dụng trên môi trường production.", "ERR_RATE_LIMIT_UNAVAILABLE");
        }

        checkWithInMemory(cleanEmail, cleanIp);
    }

    private void checkCount(String key, int max, String msg) {
        String countStr = redisTemplate.opsForValue().get(key);
        if (countStr != null && Integer.parseInt(countStr) >= max) {
            throw new IdentityDomainException(msg, "ERR_RATE_LIMIT_EXCEEDED");
        }
    }

    private void incrementWithExpire(String key, int windowSecs) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSecs));
        }
    }

    private synchronized void checkWithInMemory(String email, String ip) {
        Instant now = Instant.now();
        if (inMemoryCooldowns.getOrDefault(email, Instant.MIN).isAfter(now) || inMemoryCooldowns.getOrDefault(ip, Instant.MIN).isAfter(now)) {
            throw new IdentityDomainException("Vui lòng đợi ít nhất 60 giây trước khi yêu cầu lại mã OTP.", "ERR_RATE_LIMIT_COOLDOWN");
        }
        if (inMemoryCounters.getOrDefault(email, 0) >= MAX_HOURLY_REQUESTS || inMemoryCounters.getOrDefault(ip, 0) >= MAX_HOURLY_REQUESTS) {
            throw new IdentityDomainException("Bạn đã vượt quá số lần yêu cầu mã OTP (tối đa 5 lần/giờ).", "ERR_RATE_LIMIT_EXCEEDED");
        }
        inMemoryCooldowns.put(email, now.plusSeconds(COOLDOWN_SECONDS));
        inMemoryCooldowns.put(ip, now.plusSeconds(COOLDOWN_SECONDS));
        inMemoryCounters.merge(email, 1, Integer::sum);
        inMemoryCounters.merge(ip, 1, Integer::sum);
    }

    @Override
    public void checkPasswordChangeAllowed(String userId, String ipAddress) {
        String cleanUser = userId != null ? userId : "unknown";
        if (isRedisAlive()) {
            try {
                if (Boolean.TRUE.equals(redisTemplate.hasKey("rate:pwd:lock:user:" + cleanUser))) {
                    throw new IdentityDomainException("Ban da nhap sai mat khau qua 5 lan. Vui long thu lai sau 15 phut.", "ERR_AUTH_PASSWORD_CHANGE_LOCKED");
                }
                return;
            } catch (IdentityDomainException e) {
                throw e;
            } catch (Exception ex) {
                handleProdFailClosed("pwd_lock", ex);
            }
        }
        if (isProd()) {
            throw new IdentityDomainException("Hệ thống kiểm soát tần suất không khả dụng trên môi trường production.", "ERR_RATE_LIMIT_UNAVAILABLE");
        }
        if (inMemoryPasswordLocks.getOrDefault(cleanUser, Instant.MIN).isAfter(Instant.now())) {
            throw new IdentityDomainException("Ban da nhap sai mat khau qua 5 lan. Vui long thu lai sau 15 phut.", "ERR_AUTH_PASSWORD_CHANGE_LOCKED");
        }
    }

    @Override
    public void recordFailedPasswordChange(String userId, String ipAddress) {
        String cleanUser = userId != null ? userId : "unknown";
        if (isRedisAlive()) {
            try {
                Long attempts = redisTemplate.opsForValue().increment("rate:pwd:cnt:user:" + cleanUser);
                if (attempts != null && attempts == 1) redisTemplate.expire("rate:pwd:cnt:user:" + cleanUser, Duration.ofSeconds(PASSWORD_LOCKOUT_SECONDS));
                if (attempts != null && attempts >= MAX_FAILED_PASSWORD_ATTEMPTS) {
                    redisTemplate.opsForValue().set("rate:pwd:lock:user:" + cleanUser, "LOCKED", Duration.ofSeconds(PASSWORD_LOCKOUT_SECONDS));
                    redisTemplate.delete("rate:pwd:cnt:user:" + cleanUser);
                }
                return;
            } catch (Exception ex) {
                handleProdFailClosed("pwd_record_fail", ex);
            }
        }
        if (isProd()) {
            throw new IdentityDomainException("Hệ thống kiểm soát tần suất không khả dụng trên môi trường production.", "ERR_RATE_LIMIT_UNAVAILABLE");
        }
        int attempts = inMemoryPasswordAttempts.merge(cleanUser, 1, Integer::sum);
        if (attempts >= MAX_FAILED_PASSWORD_ATTEMPTS) {
            inMemoryPasswordLocks.put(cleanUser, Instant.now().plusSeconds(PASSWORD_LOCKOUT_SECONDS));
            inMemoryPasswordAttempts.remove(cleanUser);
        }
    }

    @Override
    public void clearPasswordChangeAttempts(String userId, String ipAddress) {
        String cleanUser = userId != null ? userId : "unknown";
        if (isRedisAlive()) {
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
}
