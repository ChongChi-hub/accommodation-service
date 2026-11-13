package vn.cozynest.identity.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GoogleAuthUseCaseTest {

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
    private GoogleAuthUseCase useCase;

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

        useCase = new GoogleAuthUseCase(
                userRepository, userIdentityRepository, oauthAccountRepository, refreshTokenRepository,
                landlordProfileRepository, auditLogRepository, tokenProvider, rateLimiter,
                googleTokenVerifier, nonceManager, loginHistoryRepository, userEventOutboxRepository,
                true, CLIENT_ID, false
        );
    }

    @Test
    @DisplayName("Status: Trả về enabled=true và fingerprint 8 ký tự SHA-256")
    void testGetStatus() {
        GoogleStatusResponse status = useCase.getStatus();
        assertThat(status.isEnabled()).isTrue();
        assertThat(status.getClientIdFingerprint()).hasSize(8);
        assertThat(status.getClientIdFingerprint()).doesNotContain(CLIENT_ID);
    }

    @Test
    @DisplayName("Người mới: Tạo user TENANT, password_hash và phone null, email_verified=true")
    void testNewUserTenantCreation() {
        GoogleNonceResponse nonceRes = useCase.generateNonce();
        String nonce = nonceRes.getNonce();

        when(googleTokenVerifier.verify("cred-123", nonce))
                .thenReturn(new GoogleTokenVerifier.GoogleTokenClaims("sub-100", "newbie@gmail.com", "New User", nonce));
        when(userIdentityRepository.findByProviderAndProviderSubject("GOOGLE", "sub-100")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newbie@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        GoogleAuthCommand cmd = GoogleAuthCommand.builder().credential("cred-123").nonce(nonce).ipAddress("127.0.0.1").build();
        AuthResult result = useCase.authenticate(cmd);

        assertThat(result.getToken()).isEqualTo("mock-access-token");
        assertThat(result.getUser().getEmail()).isEqualTo("newbie@gmail.com");
        assertThat(result.getUser().getRoles()).contains("TENANT");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeastOnce()).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isNull();
        assertThat(saved.getPhone()).isNull();
        assertThat(saved.getEmailVerifiedAt()).isNotNull();

        verify(userIdentityRepository).save(any(UserIdentity.class));
        verify(auditLogRepository).save(any(IdentityAuditLog.class));
    }

    @Test
    @DisplayName("Nonce dùng một lần: Dùng lại nonce cũ ném ERR_GOOGLE_NONCE_INVALID")
    void testSingleUseNonceReplay() {
        GoogleNonceResponse nonceRes = useCase.generateNonce();
        String nonce = nonceRes.getNonce();

        when(googleTokenVerifier.verify("cred-123", nonce))
                .thenReturn(new GoogleTokenVerifier.GoogleTokenClaims("sub-100", "newbie@gmail.com", "New User", nonce));
        when(userIdentityRepository.findByProviderAndProviderSubject("GOOGLE", "sub-100")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newbie@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        GoogleAuthCommand cmd = GoogleAuthCommand.builder().credential("cred-123").nonce(nonce).ipAddress("127.0.0.1").build();
        useCase.authenticate(cmd);

        assertThatThrownBy(() -> useCase.authenticate(cmd))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_GOOGLE_NONCE_INVALID");
    }

    @Test
    @DisplayName("Đã liên kết: Đăng nhập trực tiếp theo sub")
    void testExistingLinkedAccountLogin() {
        GoogleNonceResponse nonceRes = useCase.generateNonce();
        String nonce = nonceRes.getNonce();

        User existingUser = new User(
                "user-111", "existing@gmail.com", "0901111111", "hashed", "Existing User",
                null, null, null, UserStatus.ACTIVE, Instant.now(), null, Set.of(UserRoleType.TENANT),
                Instant.now(), Instant.now()
        );
        UserIdentity idn = UserIdentity.createGoogleIdentity("user-111", "sub-222", "existing@gmail.com", "Existing User", null, "vi");

        when(googleTokenVerifier.verify("cred-123", nonce))
                .thenReturn(new GoogleTokenVerifier.GoogleTokenClaims("sub-222", "existing@gmail.com", "Existing User", nonce));
        when(userIdentityRepository.findByProviderAndProviderSubject("GOOGLE", "sub-222")).thenReturn(Optional.of(idn));
        when(userRepository.findById("user-111")).thenReturn(Optional.of(existingUser));

        GoogleAuthCommand cmd = GoogleAuthCommand.builder().credential("cred-123").nonce(nonce).build();
        AuthResult result = useCase.authenticate(cmd);

        assertThat(result.getToken()).isEqualTo("mock-access-token");
        assertThat(result.getUser().getId()).isEqualTo("user-111");
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Email trùng đã xác minh: Tự động liên kết tài khoản")
    void testExistingVerifiedEmailAutoLink() {
        GoogleNonceResponse nonceRes = useCase.generateNonce();
        String nonce = nonceRes.getNonce();

        User verifiedUser = new User(
                "user-333", "verified@gmail.com", "0903333333", "pwd-hash", "Verified User",
                null, null, null, UserStatus.ACTIVE, Instant.now().minusSeconds(3600), null, Set.of(UserRoleType.TENANT),
                Instant.now(), Instant.now()
        );

        when(googleTokenVerifier.verify("cred-123", nonce))
                .thenReturn(new GoogleTokenVerifier.GoogleTokenClaims("sub-333", "verified@gmail.com", "Verified User", nonce));
        when(userIdentityRepository.findByProviderAndProviderSubject("GOOGLE", "sub-333")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("verified@gmail.com")).thenReturn(Optional.of(verifiedUser));

        GoogleAuthCommand cmd = GoogleAuthCommand.builder().credential("cred-123").nonce(nonce).build();
        AuthResult result = useCase.authenticate(cmd);

        assertThat(result.getUser().getId()).isEqualTo("user-333");
        verify(userIdentityRepository).save(any(UserIdentity.class));
        verify(refreshTokenRepository, never()).revokeAllByUserId(any());
    }
}
