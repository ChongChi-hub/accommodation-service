package vn.cozynest.identity.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import vn.cozynest.identity.application.dto.GoogleNonceResponse;
import vn.cozynest.identity.domain.exception.IdentityDomainException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GoogleNonceManager {

    private final StringRedisTemplate redisTemplate;
    private final boolean isProduction;
    private final ConcurrentHashMap<String, Instant> inMemoryNonces = new ConcurrentHashMap<>();

    public GoogleNonceManager(
            StringRedisTemplate redisTemplate,
            @Value("${spring.profiles.active:dev}") String activeProfile) {
        this.redisTemplate = redisTemplate;
        this.isProduction = activeProfile != null && (activeProfile.contains("prod") || activeProfile.contains("production"));
    }

    public GoogleNonceResponse generateNonce() {
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String redisKey = "google:nonce:" + nonce;
        boolean saved = false;
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                redisTemplate.opsForValue().set(redisKey, "VALID", Duration.ofMinutes(5));
                saved = true;
            }
        } catch (Exception ignored) {}
        if (!saved) {
            if (isProduction) {
                throw new IdentityDomainException("Hệ thống bảo mật nonce không khả dụng.", "ERR_INTERNAL_SERVER_ERROR");
            }
            inMemoryNonces.put(nonce, Instant.now().plus(Duration.ofMinutes(5)));
        }
        return new GoogleNonceResponse(nonce);
    }

    public void consumeNonce(String nonce) {
        if (nonce == null || nonce.isBlank()) {
            throw new IdentityDomainException("Mã xác thực nonce không hợp lệ", "ERR_GOOGLE_NONCE_INVALID");
        }
        String redisKey = "google:nonce:" + nonce;
        boolean valid = false;
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                    redisTemplate.delete(redisKey);
                    valid = true;
                }
            }
        } catch (Exception ignored) {}
        if (!valid) {
            Instant expiry = inMemoryNonces.remove(nonce);
            if (expiry != null && expiry.isAfter(Instant.now())) {
                valid = true;
            }
        }
        if (!valid) {
            throw new IdentityDomainException("Mã xác thực nonce không hợp lệ hoặc đã hết hạn", "ERR_GOOGLE_NONCE_INVALID");
        }
    }
}

