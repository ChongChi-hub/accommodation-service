package vn.cozynest.identity.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import vn.cozynest.identity.application.dto.AuthResult;
import vn.cozynest.identity.application.dto.GoogleAuthCommand;
import vn.cozynest.identity.application.dto.GoogleNonceResponse;
import vn.cozynest.identity.application.dto.GoogleStatusResponse;
import vn.cozynest.identity.application.port.RateLimiterPort;
import vn.cozynest.identity.application.port.TokenProviderPort;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.*;
import vn.cozynest.identity.domain.repository.OAuthAccountRepository;
import vn.cozynest.identity.domain.repository.RefreshTokenRepository;
import vn.cozynest.identity.domain.repository.UserRepository;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataLoginHistoryRepository;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataUserEventOutboxRepository;
import vn.cozynest.identity.infrastructure.security.GoogleTokenVerifier;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GoogleAuthUseCaseTest {

    private UserRepository userRepository;
    private OAuthAccountRepository oauthAccountRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private TokenProviderPort tokenProvider;
    private RateLimiterPort rateLimiter;
    private GoogleTokenVerifier googleTokenVerifier;
    private SpringDataLoginHistoryRepository loginHistoryRepository;
    private SpringDataUserEventOutboxRepository userEventOutboxRepository;
    private GoogleAuthUseCase useCase;

    private static final String CLIENT_ID = "test-client-id-123.apps.googleusercontent.com";

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        oauthAccountRepository = mock(OAuthAccountRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        tokenProvider = mock(TokenProviderPort.class);
        rateLimiter = mock(RateLimiterPort.class);
        googleTokenVerifier = mock(GoogleTokenVerifier.class);
        loginHistoryRepository = mock(SpringDataLoginHistoryRepository.class);
        userEventOutboxRepository = mock(SpringDataUserEventOutboxRepository.class);

        when(tokenProvider.generateAccessToken(any())).thenReturn("mock-access-token");
        when(tokenProvider.generateRefreshToken()).thenReturn("mock-refresh-token");
        when(tokenProvider.hashToken(any())).thenReturn("mock-token-hash");

        useCase = new GoogleAuthUseCase(
                userRepository,
                oauthAccountRepository,
                refreshTokenRepository,
                tokenProvider,
                rateLimiter,
                googleTokenVerifier,
                loginHistoryRepository,
                userEventOutboxRepository,
                null, // In-memory fallback
                true,
                CLIENT_ID,
                "dev"
        );
    }

    @Test
    @DisplayName("Status: Trả về enabled=true và fingerprint 8 ký tự SHA-256 không lộ Client ID")
    void testGetStatus() {
        GoogleStatusResponse status = useCase.getStatus();
        assertThat(status.isEnabled()).isTrue();
        assertThat(status.getClientIdFingerprint()).hasSize(8);
        assertThat(status.getClientIdFingerprint()).doesNotContain(CLIENT_ID);
    }

    @Test
    @DisplayName("Người mới: Tạo user mới TENANT, password_hash và phone null, email_verified=true")
    void testNewUserTenantCreation() {
        GoogleNonceResponse nonceRes = useCase.generateNonce();
        String nonce = nonceRes.getNonce();

        when(googleTokenVerifier.verify("cred-123", nonce))
                .thenReturn(new GoogleTokenVerifier.GoogleTokenClaims("sub-100", "newbie@gmail.com", "New User", nonce));
        when(oauthAccountRepository.findByProviderAndProviderUserId("GOOGLE", "sub-100")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newbie@gmail.com")).thenReturn(Optional.empty());

        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        GoogleAuthCommand cmd = GoogleAuthCommand.builder()
                .credential("cred-123")
                .nonce(nonce)
                .ipAddress("127.0.0.1")
                .build();

        AuthResult result = useCase.authenticate(cmd);

        assertThat(result.getToken()).isEqualTo("mock-access-token");
        assertThat(result.getUser().getEmail()).isEqualTo("newbie@gmail.com");
        assertThat(result.getUser().getRoles()).contains("TENANT");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeastOnce()).save(userCaptor.capture());
        User createdUser = userCaptor.getValue();
        assertThat(createdUser.getPasswordHash()).isNull();
        assertThat(createdUser.getPhone()).isNull();
        assertThat(createdUser.getEmailVerifiedAt()).isNotNull();

        verify(oauthAccountRepository).save(any(OAuthAccount.class));
        verify(userEventOutboxRepository).save(any());
        verify(loginHistoryRepository).save(any());
    }

    @Test
    @DisplayName("Nonce dùng một lần: Dùng lại nonce cũ -> ném ERR_INVALID_NONCE")
    void testSingleUseNonceReplay() {
        GoogleNonceResponse nonceRes = useCase.generateNonce();
        String nonce = nonceRes.getNonce();

        when(googleTokenVerifier.verify("cred-123", nonce))
                .thenReturn(new GoogleTokenVerifier.GoogleTokenClaims("sub-100", "newbie@gmail.com", "New User", nonce));
        when(oauthAccountRepository.findByProviderAndProviderUserId("GOOGLE", "sub-100")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newbie@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        GoogleAuthCommand cmd = GoogleAuthCommand.builder()
                .credential("cred-123")
                .nonce(nonce)
                .ipAddress("127.0.0.1")
                .build();

        // First use: success
        useCase.authenticate(cmd);

        // Second use with same nonce: rejected!
        assertThatThrownBy(() -> useCase.authenticate(cmd))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_INVALID_NONCE");
    }

    @Test
    @DisplayName("Đã liên kết: Đăng nhập trực tiếp, không tạo user mới")
    void testExistingLinkedAccountLogin() {
        GoogleNonceResponse nonceRes = useCase.generateNonce();
        String nonce = nonceRes.getNonce();

        User existingUser = new User(
                "user-111", "existing@gmail.com", "0901111111", "hashed", "Existing User",
                null, null, null, UserStatus.ACTIVE, Instant.now(), null, Set.of(UserRoleType.TENANT),
                Instant.now(), Instant.now()
        );

        when(googleTokenVerifier.verify("cred-123", nonce))
                .thenReturn(new GoogleTokenVerifier.GoogleTokenClaims("sub-222", "existing@gmail.com", "Existing User", nonce));
        when(oauthAccountRepository.findByProviderAndProviderUserId("GOOGLE", "sub-222"))
                .thenReturn(Optional.of(OAuthAccount.create("user-111", "GOOGLE", "sub-222", "existing@gmail.com")));
        when(userRepository.findById("user-111")).thenReturn(Optional.of(existingUser));

        GoogleAuthCommand cmd = GoogleAuthCommand.builder()
                .credential("cred-123")
                .nonce(nonce)
                .build();

        AuthResult result = useCase.authenticate(cmd);

        assertThat(result.getToken()).isEqualTo("mock-access-token");
        assertThat(result.getUser().getId()).isEqualTo("user-111");
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Email trùng đã xác minh: Tự động liên kết OAuth vào tài khoản hiện có")
    void testExistingVerifiedEmailLinksAccount() {
        GoogleNonceResponse nonceRes = useCase.generateNonce();
        String nonce = nonceRes.getNonce();

        User verifiedUser = new User(
                "user-333", "verified@gmail.com", "0903333333", "pwd-hash", "Verified User",
                null, null, null, UserStatus.ACTIVE, Instant.now().minusSeconds(3600), null, Set.of(UserRoleType.TENANT),
                Instant.now(), Instant.now()
        );

        when(googleTokenVerifier.verify("cred-123", nonce))
                .thenReturn(new GoogleTokenVerifier.GoogleTokenClaims("sub-333", "verified@gmail.com", "Verified User", nonce));
        when(oauthAccountRepository.findByProviderAndProviderUserId("GOOGLE", "sub-333")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("verified@gmail.com")).thenReturn(Optional.of(verifiedUser));

        GoogleAuthCommand cmd = GoogleAuthCommand.builder()
                .credential("cred-123")
                .nonce(nonce)
                .build();

        AuthResult result = useCase.authenticate(cmd);

        assertThat(result.getUser().getId()).isEqualTo("user-333");
        verify(oauthAccountRepository).save(any(OAuthAccount.class));
        verify(refreshTokenRepository, never()).revokeAllByUserId(any());
    }

    @Test
    @DisplayName("Email trùng chưa xác minh: Thu hồi phiên cũ, xóa mật khẩu để chống chiếm tài khoản rồi liên kết")
    void testExistingUnverifiedEmailAntiTakeover() {
        GoogleNonceResponse nonceRes = useCase.generateNonce();
        String nonce = nonceRes.getNonce();

        User unverifiedUser = new User(
                "user-444", "unverified@gmail.com", "0904444444", "old-pwd-hash", "Unverified User",
                null, null, null, UserStatus.ACTIVE, null, null, Set.of(UserRoleType.TENANT),
                Instant.now(), Instant.now()
        );

        when(googleTokenVerifier.verify("cred-123", nonce))
                .thenReturn(new GoogleTokenVerifier.GoogleTokenClaims("sub-444", "unverified@gmail.com", "Unverified User", nonce));
        when(oauthAccountRepository.findByProviderAndProviderUserId("GOOGLE", "sub-444")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("unverified@gmail.com")).thenReturn(Optional.of(unverifiedUser));

        GoogleAuthCommand cmd = GoogleAuthCommand.builder()
                .credential("cred-123")
                .nonce(nonce)
                .build();

        AuthResult result = useCase.authenticate(cmd);

        assertThat(result.getUser().getId()).isEqualTo("user-444");
        verify(refreshTokenRepository).revokeAllByUserId("user-444");
        assertThat(unverifiedUser.getPasswordHash()).isNull();
        assertThat(unverifiedUser.getEmailVerifiedAt()).isNotNull();
        verify(oauthAccountRepository).save(any(OAuthAccount.class));
    }

    @Test
    @DisplayName("Tài khoản SUSPENDED: Bị từ chối HTTP 403 mã ERR_USER_SUSPENDED")
    void testSuspendedUserBlocked() {
        GoogleNonceResponse nonceRes = useCase.generateNonce();
        String nonce = nonceRes.getNonce();

        User suspendedUser = new User(
                "user-555", "suspended@gmail.com", "0905555555", null, "Suspended User",
                null, null, null, UserStatus.SUSPENDED, Instant.now(), null, Set.of(UserRoleType.TENANT),
                Instant.now(), Instant.now()
        );

        when(googleTokenVerifier.verify("cred-123", nonce))
                .thenReturn(new GoogleTokenVerifier.GoogleTokenClaims("sub-555", "suspended@gmail.com", "Suspended User", nonce));
        when(oauthAccountRepository.findByProviderAndProviderUserId("GOOGLE", "sub-555"))
                .thenReturn(Optional.of(OAuthAccount.create("user-555", "GOOGLE", "sub-555", "suspended@gmail.com")));
        when(userRepository.findById("user-555")).thenReturn(Optional.of(suspendedUser));

        GoogleAuthCommand cmd = GoogleAuthCommand.builder()
                .credential("cred-123")
                .nonce(nonce)
                .build();

        assertThatThrownBy(() -> useCase.authenticate(cmd))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_USER_SUSPENDED");
    }

    @Test
    @DisplayName("intendedRole = ADMIN: Bị chặn 400 ERR_INVALID_ROLE")
    void testInvalidIntendedRoleAdminBlocked() {
        GoogleNonceResponse nonceRes = useCase.generateNonce();
        String nonce = nonceRes.getNonce();

        when(googleTokenVerifier.verify("cred-123", nonce))
                .thenReturn(new GoogleTokenVerifier.GoogleTokenClaims("sub-666", "hacker@gmail.com", "Hacker", nonce));
        when(oauthAccountRepository.findByProviderAndProviderUserId("GOOGLE", "sub-666")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("hacker@gmail.com")).thenReturn(Optional.empty());

        GoogleAuthCommand cmd = GoogleAuthCommand.builder()
                .credential("cred-123")
                .nonce(nonce)
                .intendedRole("ADMIN")
                .build();

        assertThatThrownBy(() -> useCase.authenticate(cmd))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_INVALID_ROLE");
    }

    @Test
    @DisplayName("intendedRole = LANDLORD: Áp dụng cho người mới, nhưng bỏ qua với người cũ")
    void testIntendedRoleLandlordBehavior() {
        // 1. New user with LANDLORD -> gets LANDLORD role
        GoogleNonceResponse nonceRes1 = useCase.generateNonce();
        String nonce1 = nonceRes1.getNonce();

        when(googleTokenVerifier.verify("cred-1", nonce1))
                .thenReturn(new GoogleTokenVerifier.GoogleTokenClaims("sub-777", "landlord@gmail.com", "Host", nonce1));
        when(oauthAccountRepository.findByProviderAndProviderUserId("GOOGLE", "sub-777")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("landlord@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        GoogleAuthCommand cmd1 = GoogleAuthCommand.builder()
                .credential("cred-1")
                .nonce(nonce1)
                .intendedRole("LANDLORD")
                .build();

        AuthResult res1 = useCase.authenticate(cmd1);
        assertThat(res1.getUser().getRoles()).contains("LANDLORD");

        // 2. Existing TENANT user -> intendedRole LANDLORD is ignored
        GoogleNonceResponse nonceRes2 = useCase.generateNonce();
        String nonce2 = nonceRes2.getNonce();

        User existingTenant = new User(
                "user-888", "oldtenant@gmail.com", "0908888888", "pwd", "Old Tenant",
                null, null, null, UserStatus.ACTIVE, Instant.now(), null, Set.of(UserRoleType.TENANT),
                Instant.now(), Instant.now()
        );

        when(googleTokenVerifier.verify("cred-2", nonce2))
                .thenReturn(new GoogleTokenVerifier.GoogleTokenClaims("sub-888", "oldtenant@gmail.com", "Old Tenant", nonce2));
        when(oauthAccountRepository.findByProviderAndProviderUserId("GOOGLE", "sub-888"))
                .thenReturn(Optional.of(OAuthAccount.create("user-888", "GOOGLE", "sub-888", "oldtenant@gmail.com")));
        when(userRepository.findById("user-888")).thenReturn(Optional.of(existingTenant));

        GoogleAuthCommand cmd2 = GoogleAuthCommand.builder()
                .credential("cred-2")
                .nonce(nonce2)
                .intendedRole("LANDLORD")
                .build();

        AuthResult res2 = useCase.authenticate(cmd2);
        assertThat(res2.getUser().getRoles()).contains("TENANT");
        assertThat(res2.getUser().getRoles()).doesNotContain("LANDLORD");
    }
}

