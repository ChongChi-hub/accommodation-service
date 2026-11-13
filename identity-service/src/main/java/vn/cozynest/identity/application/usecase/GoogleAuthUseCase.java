package vn.cozynest.identity.application.usecase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.application.dto.*;
import vn.cozynest.identity.application.port.RateLimiterPort;
import vn.cozynest.identity.application.port.TokenProviderPort;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.*;
import vn.cozynest.identity.domain.repository.*;
import vn.cozynest.identity.infrastructure.persistence.entity.LoginHistoryJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.entity.UserEventOutboxJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataLoginHistoryRepository;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataUserEventOutboxRepository;
import vn.cozynest.identity.infrastructure.security.GoogleNonceManager;
import vn.cozynest.identity.infrastructure.security.GoogleTokenVerifier;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class GoogleAuthUseCase {
    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LandlordProfileRepository landlordProfileRepository;
    private final IdentityAuditLogRepository auditLogRepository;
    private final TokenProviderPort tokenProvider;
    private final RateLimiterPort rateLimiter;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final GoogleNonceManager nonceManager;
    private final SpringDataLoginHistoryRepository loginHistoryRepository;
    private final SpringDataUserEventOutboxRepository userEventOutboxRepository;
    private final boolean enabled;
    private final String clientId;
    private final boolean allowStaff;

    public GoogleAuthUseCase(
            UserRepository userRepository,
            UserIdentityRepository userIdentityRepository,
            OAuthAccountRepository oauthAccountRepository,
            RefreshTokenRepository refreshTokenRepository,
            LandlordProfileRepository landlordProfileRepository,
            IdentityAuditLogRepository auditLogRepository,
            TokenProviderPort tokenProvider,
            RateLimiterPort rateLimiter,
            GoogleTokenVerifier googleTokenVerifier,
            GoogleNonceManager nonceManager,
            SpringDataLoginHistoryRepository loginHistoryRepository,
            SpringDataUserEventOutboxRepository userEventOutboxRepository,
            @Value("${google.enabled:false}") boolean enabled,
            @Value("${google.client-id:}") String clientId,
            @Value("${auth.google.allow-staff:false}") boolean allowStaff) {
        this.userRepository = userRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.oauthAccountRepository = oauthAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.landlordProfileRepository = landlordProfileRepository;
        this.auditLogRepository = auditLogRepository;
        this.tokenProvider = tokenProvider;
        this.rateLimiter = rateLimiter;
        this.googleTokenVerifier = googleTokenVerifier;
        this.nonceManager = nonceManager;
        this.loginHistoryRepository = loginHistoryRepository;
        this.userEventOutboxRepository = userEventOutboxRepository;
        this.enabled = enabled;
        this.clientId = clientId != null ? clientId.trim() : "";
        this.allowStaff = allowStaff;
    }

    public GoogleNonceResponse generateNonce() {
        return nonceManager.generateNonce();
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
        if (!enabled || clientId.isBlank()) {
            throw new IdentityDomainException("Tính năng đăng nhập Google chưa được cấu hình.", "ERR_GOOGLE_NOT_CONFIGURED");
        }

        nonceManager.consumeNonce(command.getNonce());
        GoogleTokenVerifier.GoogleTokenClaims claims = googleTokenVerifier.verify(command.getCredential(), command.getNonce());

        boolean[] isNewUserHolder = new boolean[]{false};
        User user = resolveUser(claims, command.getIntendedRole(), command.getIpAddress(), command.getDeviceInfo(), isNewUserHolder);

        if (user.getStatus() == UserStatus.DELETED) {
            throw new IdentityDomainException("Tài khoản đã bị xóa khỏi hệ thống", "ERR_USER_DELETED");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new IdentityDomainException("Tài khoản của bạn đang bị đình chỉ hoạt động", "ERR_USER_SUSPENDED");
        }

        user.recordLogin();
        userRepository.save(user);

        String accessToken = tokenProvider.generateAccessToken(user);
        String rawRefreshToken = tokenProvider.generateRefreshToken();
        RefreshToken refreshToken = RefreshToken.create(
                user.getId(), tokenProvider.hashToken(rawRefreshToken),
                command.getDeviceInfo() != null ? command.getDeviceInfo() : "Google Web",
                command.getIpAddress() != null ? command.getIpAddress() : "127.0.0.1",
                Instant.now().plus(30, ChronoUnit.DAYS));
        refreshTokenRepository.save(refreshToken);

        loginHistoryRepository.save(LoginHistoryJpaEntity.builder()
                .id(UUID.randomUUID().toString()).userId(user.getId()).method("GOOGLE")
                .ipAddress(command.getIpAddress()).userAgent(command.getDeviceInfo()).createdAt(Instant.now()).build());

        LandlordProfile lp = landlordProfileRepository.findByUserId(user.getId()).orElse(null);
        return AuthResult.builder()
                .token(accessToken).refreshToken(rawRefreshToken)
                .user(UserDto.fromDomain(user, lp))
                .roles(user.getRoles().stream().map(Enum::name).toList())
                .isNewUser(isNewUserHolder[0])
                .hasPassword(user.hasPassword())
                .emailVerified(user.getEmailVerifiedAt() != null)
                .landlordApplication(LandlordApplicationSummaryDto.fromDomain(lp))
                .build();
    }

    private User resolveUser(GoogleTokenVerifier.GoogleTokenClaims claims, String intendedRole, String ip, String ua, boolean[] isNewHolder) {
        Optional<UserIdentity> existingIdentity = userIdentityRepository.findByProviderAndProviderSubject("GOOGLE", claims.sub());
        if (existingIdentity.isPresent()) {
            UserIdentity idn = existingIdentity.get();
            idn.updateLogin(claims.name(), claims.pictureUrl(), claims.locale());
            userIdentityRepository.save(idn);
            auditLogRepository.save(IdentityAuditLog.create(idn.getUserId(), idn.getUserId(), "GOOGLE_LOGIN", "GOOGLE", ip, ua, "Google login"));
            return userRepository.findById(idn.getUserId())
                    .orElseThrow(() -> new IdentityDomainException("Tài khoản người dùng liên kết không tồn tại", "ERR_AUTH_USER_NOT_FOUND"));
        }

        String email = claims.email().trim().toLowerCase();
        Optional<User> existingUserOpt = userRepository.findByEmail(email);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            boolean isStaff = existingUser.getRoles().contains(UserRoleType.ADMIN) || existingUser.getRoles().contains(UserRoleType.MODERATOR);
            if (isStaff && !allowStaff) {
                throw new IdentityDomainException("Tài khoản nhân sự quản trị không được phép liên kết hoặc đăng nhập qua Google", "ERR_GOOGLE_NOT_ALLOWED_FOR_STAFF");
            }
            if (existingUser.getEmailVerifiedAt() == null) {
                refreshTokenRepository.revokeAllByUserId(existingUser.getId());
                existingUser.clearPassword();
                existingUser.verifyEmailNow();
                userRepository.save(existingUser);
            }
            saveIdentitySafely(existingUser.getId(), claims);
            auditLogRepository.save(IdentityAuditLog.create(existingUser.getId(), existingUser.getId(), "ACCOUNT_AUTO_LINKED", "GOOGLE", ip, ua, "Auto-linked by verified email"));
            return existingUser;
        }

        return registerNewGoogleUser(claims, intendedRole, email, ip, ua, isNewHolder);
    }

    private User registerNewGoogleUser(GoogleTokenVerifier.GoogleTokenClaims claims, String intendedRole, String email, String ip, String ua, boolean[] isNewHolder) {
        String roleStr = intendedRole != null ? intendedRole.trim().toUpperCase() : "TENANT";
        if ("ADMIN".equals(roleStr) || "MODERATOR".equals(roleStr)) {
            throw new IdentityDomainException("Không được phép tự đăng ký vai trò quản trị", "ERR_INVALID_ROLE");
        }
        if (!"TENANT".equals(roleStr) && !"LANDLORD".equals(roleStr)) {
            roleStr = "TENANT";
        }

        String sanitizedName = sanitizeName(claims.name(), email);
        User newUser = User.createNewGoogleUser(email, sanitizedName, UserRoleType.TENANT);
        newUser.verifyEmailNow();

        try {
            User saved = userRepository.save(newUser);
            saveIdentitySafely(saved.getId(), claims);
            isNewHolder[0] = true;

            if ("LANDLORD".equals(roleStr)) {
                landlordProfileRepository.save(LandlordProfile.createNew(saved.getId()));
            }

            userEventOutboxRepository.save(UserEventOutboxJpaEntity.builder()
                    .id(UUID.randomUUID().toString()).aggregateId(saved.getId()).eventType("UserRegistered")
                    .payload(String.format("{\"userId\":\"%s\",\"email\":\"%s\",\"role\":\"TENANT\",\"source\":\"GOOGLE\",\"createdAt\":\"%s\"}", saved.getId(), email, Instant.now()))
                    .published(false).createdAt(Instant.now()).build());

            auditLogRepository.save(IdentityAuditLog.create(saved.getId(), saved.getId(), "GOOGLE_LINKED", "GOOGLE", ip, ua, "New Google user registration"));
            return saved;
        } catch (DataIntegrityViolationException ex) {
            return userIdentityRepository.findByProviderAndProviderSubject("GOOGLE", claims.sub())
                    .flatMap(id -> userRepository.findById(id.getUserId()))
                    .or(() -> userRepository.findByEmail(email))
                    .orElseThrow(() -> ex);
        }
    }

    private String sanitizeName(String rawName, String email) {
        if (rawName == null || rawName.isBlank()) return email.split("@")[0];
        String cleaned = rawName.replaceAll("<[^>]*>", "").trim();
        if (cleaned.length() < 2) return email.split("@")[0];
        return cleaned.length() > 60 ? cleaned.substring(0, 60) : cleaned;
    }

    private void saveIdentitySafely(String userId, GoogleTokenVerifier.GoogleTokenClaims claims) {
        try {
            userIdentityRepository.save(UserIdentity.createGoogleIdentity(
                    userId, claims.sub(), claims.email(), claims.name(), claims.pictureUrl(), claims.locale()));
            oauthAccountRepository.save(OAuthAccount.create(userId, "GOOGLE", claims.sub(), claims.email()));
        } catch (DataIntegrityViolationException ignored) {}
    }
}
