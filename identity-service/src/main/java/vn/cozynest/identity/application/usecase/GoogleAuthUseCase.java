package vn.cozynest.identity.application.usecase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.dto.*;
import vn.cozynest.identity.application.port.RateLimiterPort;
import vn.cozynest.identity.application.port.TokenProviderPort;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.*;
import vn.cozynest.identity.domain.repository.OAuthAccountRepository;
import vn.cozynest.identity.domain.repository.RefreshTokenRepository;
import vn.cozynest.identity.domain.repository.UserRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.LoginHistoryJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.entity.UserEventOutboxJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataLoginHistoryRepository;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataUserEventOutboxRepository;
import vn.cozynest.identity.infrastructure.security.GoogleTokenVerifier;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GoogleAuthUseCase {

    private final UserRepository userRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProviderPort tokenProvider;
    private final RateLimiterPort rateLimiter;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final SpringDataLoginHistoryRepository loginHistoryRepository;
    private final SpringDataUserEventOutboxRepository userEventOutboxRepository;
    private final StringRedisTemplate redisTemplate;

    private final boolean enabled;
    private final String clientId;
    private final boolean isProduction;

    // In-memory fallback for non-production environments when Redis is not available
    private final ConcurrentHashMap<String, Instant> inMemoryNonces = new ConcurrentHashMap<>();

    public GoogleAuthUseCase(
            UserRepository userRepository,
            OAuthAccountRepository oauthAccountRepository,
            RefreshTokenRepository refreshTokenRepository,
            TokenProviderPort tokenProvider,
            RateLimiterPort rateLimiter,
            GoogleTokenVerifier googleTokenVerifier,
            SpringDataLoginHistoryRepository loginHistoryRepository,
            SpringDataUserEventOutboxRepository userEventOutboxRepository,
            StringRedisTemplate redisTemplate,
            @Value("${google.enabled:false}") boolean enabled,
            @Value("${google.client-id:}") String clientId,
            @Value("${spring.profiles.active:dev}") String activeProfile) {
        this.userRepository = userRepository;
        this.oauthAccountRepository = oauthAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenProvider = tokenProvider;
        this.rateLimiter = rateLimiter;
        this.googleTokenVerifier = googleTokenVerifier;
        this.loginHistoryRepository = loginHistoryRepository;
        this.userEventOutboxRepository = userEventOutboxRepository;
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
        this.clientId = clientId != null ? clientId.trim() : "";
        this.isProduction = activeProfile != null && (activeProfile.contains("prod") || activeProfile.contains("production"));
    }

    public GoogleNonceResponse generateNonce() {
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String redisKey = "google:nonce:" + nonce;

        boolean savedToRedis = false;
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                redisTemplate.opsForValue().set(redisKey, "VALID", Duration.ofMinutes(5));
                savedToRedis = true;
            }
        } catch (Exception ex) {
            if (isProduction) {
                throw new IdentityDomainException("Hệ thống kiểm soát bảo mật nonce không khả dụng trên môi trường production.", "ERR_INTERNAL_SERVER_ERROR");
            }
        }

        if (!savedToRedis) {
            if (isProduction) {
                throw new IdentityDomainException("Hệ thống kiểm soát bảo mật nonce không khả dụng trên môi trường production.", "ERR_INTERNAL_SERVER_ERROR");
            }
            inMemoryNonces.put(nonce, Instant.now().plus(Duration.ofMinutes(5)));
        }

        return new GoogleNonceResponse(nonce);
    }

    public GoogleStatusResponse getStatus() {
        boolean isConfigured = enabled && !clientId.isBlank();
        String fingerprint = "";

        if (isConfigured) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(clientId.getBytes(StandardCharsets.UTF_8));
                StringBuilder hex = new StringBuilder();
                for (int i = 0; i < 4; i++) { // 4 bytes = 8 hex chars
                    String h = Integer.toHexString(0xff & hash[i]);
                    if (h.length() == 1) hex.append('0');
                    hex.append(h);
                }
                fingerprint = hex.toString();
            } catch (Exception ignored) {}
        }

        return new GoogleStatusResponse(isConfigured, fingerprint);
    }

    @Transactional
    public AuthResult authenticate(GoogleAuthCommand command) {
        // 1. Rate limit check (10 req/min/IP)
        rateLimiter.checkGoogleAuthRateLimit(command.getIpAddress());

        // 2. Configuration check
        if (!enabled || clientId.isBlank()) {
            throw new IdentityDomainException("Tính năng đăng nhập bằng Google chưa được cấu hình.", "ERR_GOOGLE_NOT_CONFIGURED");
        }

        // 3. Nonce verification and immediate single-use consumption
        String nonce = command.getNonce();
        String redisKey = "google:nonce:" + nonce;
        boolean nonceValid = false;

        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                Boolean hasKey = redisTemplate.hasKey(redisKey);
                if (Boolean.TRUE.equals(hasKey)) {
                    redisTemplate.delete(redisKey);
                    nonceValid = true;
                }
            }
        } catch (Exception ex) {
            if (isProduction) {
                throw new IdentityDomainException("Lỗi kiểm tra bảo mật nonce trên môi trường production.", "ERR_INTERNAL_SERVER_ERROR");
            }
        }

        if (!nonceValid) {
            Instant expiry = inMemoryNonces.remove(nonce);
            if (expiry != null && expiry.isAfter(Instant.now())) {
                nonceValid = true;
            }
        }

        if (!nonceValid) {
            throw new IdentityDomainException("Mã xác thực nonce không hợp lệ hoặc đã hết hạn", "ERR_INVALID_NONCE");
        }

        // 4. Verify token cryptographic signature and claims
        GoogleTokenVerifier.GoogleTokenClaims claims = googleTokenVerifier.verify(command.getCredential(), nonce);

        // 5. Account resolution
        User user = resolveUser(claims, command.getIntendedRole());

        // 6. Security status check
        if (user.getStatus() == UserStatus.DELETED) {
            throw new IdentityDomainException("Tài khoản đã bị xóa khỏi hệ thống", "ERR_USER_DELETED");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new IdentityDomainException("Tài khoản của bạn đang bị đình chỉ hoạt động", "ERR_USER_SUSPENDED");
        }

        // 7. Record login and generate auth tokens
        user.recordLogin();
        userRepository.save(user);

        String accessToken = tokenProvider.generateAccessToken(user);
        String rawRefreshToken = tokenProvider.generateRefreshToken();
        String tokenHash = tokenProvider.hashToken(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.create(
                user.getId(),
                tokenHash,
                command.getDeviceInfo() != null ? command.getDeviceInfo() : "Google Web",
                command.getIpAddress() != null ? command.getIpAddress() : "127.0.0.1",
                Instant.now().plus(30, ChronoUnit.DAYS)
        );
        refreshTokenRepository.save(refreshToken);

        // 8. Record login history
        loginHistoryRepository.save(LoginHistoryJpaEntity.builder()
                .id(UUID.randomUUID().toString())
                .userId(user.getId())
                .method("GOOGLE")
                .ipAddress(command.getIpAddress())
                .userAgent(command.getDeviceInfo())
                .createdAt(Instant.now())
                .build());

        return AuthResult.builder()
                .token(accessToken)
                .refreshToken(rawRefreshToken)
                .user(UserDto.fromDomain(user))
                .build();
    }

    private User resolveUser(GoogleTokenVerifier.GoogleTokenClaims claims, String intendedRole) {
        // Step A: Search by (GOOGLE, sub)
        var existingOAuth = oauthAccountRepository.findByProviderAndProviderUserId("GOOGLE", claims.sub());
        if (existingOAuth.isPresent()) {
            return userRepository.findById(existingOAuth.get().getUserId())
                    .orElseThrow(() -> new IdentityDomainException("Tài khoản người dùng liên kết không tồn tại", "ERR_AUTH_USER_NOT_FOUND"));
        }

        // Step B: Search by normalized email
        String email = claims.email();
        var existingUserOpt = userRepository.findByEmail(email);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            if (existingUser.getEmailVerifiedAt() != null) {
                // Email already verified: link OAuth account directly
                saveOAuthAccountSafely(existingUser.getId(), claims.sub(), email);
            } else {
                // Email unverified: prevent account takeover by revoking sessions, clearing password, and marking verified
                refreshTokenRepository.revokeAllByUserId(existingUser.getId());
                existingUser.clearPassword();
                existingUser.verifyEmailNow();
                userRepository.save(existingUser);
                saveOAuthAccountSafely(existingUser.getId(), claims.sub(), email);
            }
            return existingUser;
        }

        // Step C: New user creation
        UserRoleType role = UserRoleType.TENANT;
        if (intendedRole != null && !intendedRole.isBlank()) {
            String roleUpper = intendedRole.trim().toUpperCase();
            if (!"TENANT".equals(roleUpper) && !"LANDLORD".equals(roleUpper)) {
                throw new IdentityDomainException("Vai trò đăng ký không hợp lệ: " + intendedRole, "ERR_INVALID_ROLE");
            }
            role = UserRoleType.fromString(roleUpper);
        }

        User newUser = User.createNewGoogleUser(email, claims.name(), role);
        try {
            User saved = userRepository.save(newUser);
            saveOAuthAccountSafely(saved.getId(), claims.sub(), email);

            // Emit UserRegistered event to outbox
            String payload = String.format("{\"userId\":\"%s\",\"email\":\"%s\",\"role\":\"%s\",\"source\":\"GOOGLE\",\"createdAt\":\"%s\"}",
                    saved.getId(), email, role.name(), Instant.now());
            userEventOutboxRepository.save(UserEventOutboxJpaEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .aggregateId(saved.getId())
                    .eventType("UserRegistered")
                    .payload(payload)
                    .published(false)
                    .createdAt(Instant.now())
                    .build());

            return saved;
        } catch (DataIntegrityViolationException ex) {
            // Concurrent registration with same sub or email: load existing
            return oauthAccountRepository.findByProviderAndProviderUserId("GOOGLE", claims.sub())
                    .flatMap(oa -> userRepository.findById(oa.getUserId()))
                    .or(() -> userRepository.findByEmail(email))
                    .orElseThrow(() -> ex);
        }
    }

    private void saveOAuthAccountSafely(String userId, String sub, String email) {
        try {
            oauthAccountRepository.save(OAuthAccount.create(userId, "GOOGLE", sub, email));
        } catch (DataIntegrityViolationException ignored) {
            // Already linked by concurrent request
        }
    }
}

