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
import vn.cozynest.identity.domain.repository.LandlordProfileRepository;
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
    private final LandlordProfileRepository landlordProfileRepository;
    private final TokenProviderPort tokenProvider;
    private final RateLimiterPort rateLimiter;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final SpringDataLoginHistoryRepository loginHistoryRepository;
    private final SpringDataUserEventOutboxRepository userEventOutboxRepository;
    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;
    private final String clientId;
    private final boolean isProduction;
    private final ConcurrentHashMap<String, Instant> inMemoryNonces = new ConcurrentHashMap<>();

    public GoogleAuthUseCase(
            UserRepository userRepository,
            OAuthAccountRepository oauthAccountRepository,
            RefreshTokenRepository refreshTokenRepository,
            LandlordProfileRepository landlordProfileRepository,
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
        this.landlordProfileRepository = landlordProfileRepository;
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
        boolean saved = false;
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                redisTemplate.opsForValue().set(redisKey, "VALID", Duration.ofMinutes(5));
                saved = true;
            }
        } catch (Exception ignored) {}
        if (!saved) {
            if (isProduction) throw new IdentityDomainException("Hệ thống bảo mật nonce không khả dụng.", "ERR_INTERNAL_SERVER_ERROR");
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
                for (int i = 0; i < 4; i++) {
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
        rateLimiter.checkGoogleAuthRateLimit(command.getIpAddress());
        if (!enabled || clientId.isBlank()) throw new IdentityDomainException("Tính năng đăng nhập Google chưa được cấu hình.", "ERR_GOOGLE_NOT_CONFIGURED");

        String nonce = command.getNonce();
        consumeNonce(nonce);

        GoogleTokenVerifier.GoogleTokenClaims claims = googleTokenVerifier.verify(command.getCredential(), nonce);
        boolean[] isNewUserHolder = new boolean[]{false};
        User user = resolveUser(claims, command.getIntendedRole(), isNewUserHolder);

        if (user.getStatus() == UserStatus.DELETED) throw new IdentityDomainException("Tài khoản đã bị xóa khỏi hệ thống", "ERR_USER_DELETED");
        if (user.getStatus() == UserStatus.SUSPENDED) throw new IdentityDomainException("Tài khoản của bạn đang bị đình chỉ hoạt động", "ERR_USER_SUSPENDED");

        user.recordLogin();
        userRepository.save(user);

        String accessToken = tokenProvider.generateAccessToken(user);
        String rawRefreshToken = tokenProvider.generateRefreshToken();
        String tokenHash = tokenProvider.hashToken(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.create(
                user.getId(), tokenHash,
                command.getDeviceInfo() != null ? command.getDeviceInfo() : "Google Web",
                command.getIpAddress() != null ? command.getIpAddress() : "127.0.0.1",
                Instant.now().plus(30, ChronoUnit.DAYS)
        );
        refreshTokenRepository.save(refreshToken);

        loginHistoryRepository.save(LoginHistoryJpaEntity.builder()
                .id(UUID.randomUUID().toString()).userId(user.getId()).method("GOOGLE")
                .ipAddress(command.getIpAddress()).userAgent(command.getDeviceInfo()).createdAt(Instant.now()).build());

        LandlordProfile lp = landlordProfileRepository.findByUserId(user.getId()).orElse(null);
        boolean isNew = isNewUserHolder[0];

        return AuthResult.builder()
                .token(accessToken)
                .refreshToken(rawRefreshToken)
                .user(UserDto.fromDomain(user, lp))
                .roles(user.getRoles().stream().map(Enum::name).toList())
                .isNewUser(isNew)
                .emailVerified(user.getEmailVerifiedAt() != null)
                .landlordApplication(LandlordApplicationSummaryDto.fromDomain(lp))
                .build();
    }

    private void consumeNonce(String nonce) {
        String redisKey = "google:nonce:" + nonce;
        boolean valid = false;
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                Boolean hasKey = redisTemplate.hasKey(redisKey);
                if (Boolean.TRUE.equals(hasKey)) {
                    redisTemplate.delete(redisKey);
                    valid = true;
                }
            }
        } catch (Exception ignored) {}
        if (!valid) {
            Instant expiry = inMemoryNonces.remove(nonce);
            if (expiry != null && expiry.isAfter(Instant.now())) valid = true;
        }
        if (!valid) throw new IdentityDomainException("Mã xác thực nonce không hợp lệ hoặc đã hết hạn", "ERR_INVALID_NONCE");
    }

    private User resolveUser(GoogleTokenVerifier.GoogleTokenClaims claims, String intendedRole, boolean[] isNewHolder) {
        var existingOAuth = oauthAccountRepository.findByProviderAndProviderUserId("GOOGLE", claims.sub());
        if (existingOAuth.isPresent()) {
            return userRepository.findById(existingOAuth.get().getUserId())
                    .orElseThrow(() -> new IdentityDomainException("Tài khoản người dùng liên kết không tồn tại", "ERR_AUTH_USER_NOT_FOUND"));
        }

        String email = claims.email().trim().toLowerCase();
        var existingUserOpt = userRepository.findByEmail(email);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (existingUser.getEmailVerifiedAt() == null) {
                refreshTokenRepository.revokeAllByUserId(existingUser.getId());
                existingUser.clearPassword();
                existingUser.verifyEmailNow();
                userRepository.save(existingUser);
            }
            saveOAuthAccountSafely(existingUser.getId(), claims.sub(), email);
            return existingUser;
        }

        // New user registration via Google
        String roleStr = intendedRole != null ? intendedRole.trim().toUpperCase() : "TENANT";
        if ("ADMIN".equals(roleStr) || "MODERATOR".equals(roleStr)) {
            throw new IdentityDomainException("Không được phép tự đăng ký vai trò quản trị", "ERR_INVALID_ROLE");
        }
        if (!"TENANT".equals(roleStr) && !"LANDLORD".equals(roleStr)) {
            throw new IdentityDomainException("Vai trò đăng ký không hợp lệ: " + intendedRole, "ERR_INVALID_ROLE");
        }

        // R1: New self-registered user receives TENANT role only
        User newUser = User.createNewGoogleUser(email, claims.name(), UserRoleType.TENANT);
        newUser.verifyEmailNow();
        try {
            User saved = userRepository.save(newUser);
            saveOAuthAccountSafely(saved.getId(), claims.sub(), email);
            isNewHolder[0] = true;

            // R2: If intendedRole is LANDLORD, create AWAITING_KYC landlord profile
            if ("LANDLORD".equals(roleStr)) {
                landlordProfileRepository.save(LandlordProfile.createNew(saved.getId()));
            }

            userEventOutboxRepository.save(UserEventOutboxJpaEntity.builder()
                    .id(UUID.randomUUID().toString()).aggregateId(saved.getId()).eventType("UserRegistered")
                    .payload(String.format("{\"userId\":\"%s\",\"email\":\"%s\",\"role\":\"TENANT\",\"source\":\"GOOGLE\",\"createdAt\":\"%s\"}",
                            saved.getId(), email, Instant.now()))
                    .published(false).createdAt(Instant.now()).build());

            return saved;
        } catch (DataIntegrityViolationException ex) {
            return oauthAccountRepository.findByProviderAndProviderUserId("GOOGLE", claims.sub())
                    .flatMap(oa -> userRepository.findById(oa.getUserId()))
                    .or(() -> userRepository.findByEmail(email))
                    .orElseThrow(() -> ex);
        }
    }

    private void saveOAuthAccountSafely(String userId, String sub, String email) {
        try {
            oauthAccountRepository.save(OAuthAccount.create(userId, "GOOGLE", sub, email));
        } catch (DataIntegrityViolationException ignored) {}
    }
}
