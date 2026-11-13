package vn.cozynest.identity.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.cozynest.identity.application.dto.*;
import vn.cozynest.identity.application.port.RateLimiterPort;
import vn.cozynest.identity.application.port.TokenProviderPort;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.*;
import vn.cozynest.identity.domain.repository.*;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataLoginHistoryRepository;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataUserEventOutboxRepository;
import vn.cozynest.identity.infrastructure.security.GoogleNonceManager;
import vn.cozynest.identity.infrastructure.security.GoogleTokenVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GoogleAuthStaffAndIdentitiesTest {

    private UserRepository userRepository;
    private UserIdentityRepository userIdentityRepository;
    private OAuthAccountRepository oauthAccountRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private LandlordProfileRepository landlordProfileRepository;
    private IdentityAuditLogRepository auditLogRepository;
    private TokenProviderPort tokenProvider;
    private RateLimiterPort rateLimiter;
    private GoogleTokenVerifier googleTokenVerifier;
    private GoogleNonceManager nonceManager;
    private SpringDataLoginHistoryRepository loginHistoryRepository;
    private SpringDataUserEventOutboxRepository userEventOutboxRepository;
    private GoogleAuthUseCase googleAuthUseCase;
    private ManageUserIdentitiesUseCase manageIdentitiesUseCase;

    private static final String CLIENT_ID = "test-client-id-123.apps.googleusercontent.com";

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userIdentityRepository = mock(UserIdentityRepository.class);
        oauthAccountRepository = mock(OAuthAccountRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        landlordProfileRepository = mock(LandlordProfileRepository.class);
        auditLogRepository = mock(IdentityAuditLogRepository.class);
        tokenProvider = mock(TokenProviderPort.class);
        rateLimiter = mock(RateLimiterPort.class);
        googleTokenVerifier = mock(GoogleTokenVerifier.class);
        nonceManager = new GoogleNonceManager(null, "dev");
        loginHistoryRepository = mock(SpringDataLoginHistoryRepository.class);
        userEventOutboxRepository = mock(SpringDataUserEventOutboxRepository.class);

        when(tokenProvider.generateAccessToken(any())).thenReturn("mock-access-token");
        when(tokenProvider.generateRefreshToken()).thenReturn("mock-refresh-token");
        when(tokenProvider.hashToken(any())).thenReturn("mock-token-hash");

        googleAuthUseCase = new GoogleAuthUseCase(
                userRepository, userIdentityRepository, oauthAccountRepository, refreshTokenRepository,
                landlordProfileRepository, auditLogRepository, tokenProvider, rateLimiter,
                googleTokenVerifier, nonceManager, loginHistoryRepository, userEventOutboxRepository,
                true, CLIENT_ID, false
        );

        manageIdentitiesUseCase = new ManageUserIdentitiesUseCase(
                userIdentityRepository, userRepository, auditLogRepository, googleTokenVerifier, nonceManager
        );
    }

    @Test
    @DisplayName("Nhân sự (ADMIN/MODERATOR): Bị chặn 403 ERR_GOOGLE_NOT_ALLOWED_FOR_STAFF")
    void testStaffAccountBlocked() {
        GoogleNonceResponse nonceRes = googleAuthUseCase.generateNonce();
        User adminUser = new User("admin-1", "admin@cozynest.vn", "0901234567", "hash", "Admin", null, null, null,
                UserStatus.ACTIVE, Instant.now(), null, Set.of(UserRoleType.ADMIN), Instant.now(), Instant.now());

        when(googleTokenVerifier.verify("cred-staff", nonceRes.getNonce()))
                .thenReturn(new GoogleTokenVerifier.GoogleTokenClaims("sub-staff", "admin@cozynest.vn", "Admin", nonceRes.getNonce()));
        when(userIdentityRepository.findByProviderAndProviderSubject("GOOGLE", "sub-staff")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@cozynest.vn")).thenReturn(Optional.of(adminUser));

        GoogleAuthCommand cmd = GoogleAuthCommand.builder().credential("cred-staff").nonce(nonceRes.getNonce()).build();

        assertThatThrownBy(() -> googleAuthUseCase.authenticate(cmd))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_GOOGLE_NOT_ALLOWED_FOR_STAFF");
    }

    @Test
    @DisplayName("Email chưa xác minh: Xóa mật khẩu, thu hồi refresh tokens chống chiếm tài khoản")
    void testUnverifiedEmailAntiTakeover() {
        GoogleNonceResponse nonceRes = googleAuthUseCase.generateNonce();
        User unverifiedUser = new User("user-unver", "unver@gmail.com", "0904444444", "old-pwd-hash", "Unver", null, null, null,
                UserStatus.ACTIVE, null, null, Set.of(UserRoleType.TENANT), Instant.now(), Instant.now());

        when(googleTokenVerifier.verify("cred-unver", nonceRes.getNonce()))
                .thenReturn(new GoogleTokenVerifier.GoogleTokenClaims("sub-unver", "unver@gmail.com", "Unver", nonceRes.getNonce()));
        when(userIdentityRepository.findByProviderAndProviderSubject("GOOGLE", "sub-unver")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("unver@gmail.com")).thenReturn(Optional.of(unverifiedUser));

        GoogleAuthCommand cmd = GoogleAuthCommand.builder().credential("cred-unver").nonce(nonceRes.getNonce()).build();
        AuthResult result = googleAuthUseCase.authenticate(cmd);

        assertThat(result.getUser().getId()).isEqualTo("user-unver");
        assertThat(unverifiedUser.getPasswordHash()).isNull();
        assertThat(unverifiedUser.getEmailVerifiedAt()).isNotNull();
        verify(refreshTokenRepository).revokeAllByUserId("user-unver");
    }

    @Test
    @DisplayName("intendedRole=LANDLORD: Tạo người dùng TENANT và hồ sơ Landlord AWAITING_KYC")
    void testIntendedRoleLandlordProfileCreation() {
        GoogleNonceResponse nonceRes = googleAuthUseCase.generateNonce();
        when(googleTokenVerifier.verify("cred-landlord", nonceRes.getNonce()))
                .thenReturn(new GoogleTokenVerifier.GoogleTokenClaims("sub-ll", "host@gmail.com", "Host", nonceRes.getNonce()));
        when(userIdentityRepository.findByProviderAndProviderSubject("GOOGLE", "sub-ll")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("host@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        GoogleAuthCommand cmd = GoogleAuthCommand.builder().credential("cred-landlord").nonce(nonceRes.getNonce()).intendedRole("LANDLORD").build();
        AuthResult result = googleAuthUseCase.authenticate(cmd);

        assertThat(result.getUser().getRoles()).containsExactly("TENANT");
        verify(landlordProfileRepository).save(any(LandlordProfile.class));
    }

    @Test
    @DisplayName("Hủy liên kết: Không có mật khẩu và chỉ có 1 identity -> ném ERR_AUTH_LAST_LOGIN_METHOD")
    void testUnlinkBlockedWhenLastLoginMethod() {
        User googleOnlyUser = new User("u-goog", "onlygoogle@gmail.com", null, null, "Only Google", null, null, null,
                UserStatus.ACTIVE, Instant.now(), null, Set.of(UserRoleType.TENANT), Instant.now(), Instant.now());
        when(userRepository.findById("u-goog")).thenReturn(Optional.of(googleOnlyUser));
        when(userIdentityRepository.countByUserId("u-goog")).thenReturn(1);

        assertThatThrownBy(() -> manageIdentitiesUseCase.unlinkGoogle("u-goog", "127.0.0.1", "Chrome"))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_AUTH_LAST_LOGIN_METHOD");
    }

    @Test
    @DisplayName("Hủy liên kết thành công khi tài khoản có mật khẩu")
    void testUnlinkSuccessWithPassword() {
        User userWithPwd = new User("u-pwd", "haspwd@gmail.com", null, "hashed-pwd", "Has Pwd", null, null, null,
                UserStatus.ACTIVE, Instant.now(), null, Set.of(UserRoleType.TENANT), Instant.now(), Instant.now());
        when(userRepository.findById("u-pwd")).thenReturn(Optional.of(userWithPwd));
        when(userIdentityRepository.countByUserId("u-pwd")).thenReturn(1);
        when(userIdentityRepository.findByUserIdAndProvider("u-pwd", "GOOGLE"))
                .thenReturn(Optional.of(UserIdentity.createGoogleIdentity("u-pwd", "sub-pwd", "haspwd@gmail.com", "Has Pwd", null, "vi")));

        manageIdentitiesUseCase.unlinkGoogle("u-pwd", "127.0.0.1", "Chrome");

        verify(userIdentityRepository).deleteByUserIdAndProvider("u-pwd", "GOOGLE");
        verify(auditLogRepository).save(any(IdentityAuditLog.class));
    }
}
