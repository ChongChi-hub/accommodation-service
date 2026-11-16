package vn.cozynest.identity.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.cozynest.identity.application.dto.AuthResult;
import vn.cozynest.identity.application.dto.LoginCommand;
import vn.cozynest.identity.application.port.PasswordEncoderPort;
import vn.cozynest.identity.application.port.TokenProviderPort;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.model.UserRoleType;
import vn.cozynest.identity.domain.repository.RefreshTokenRepository;
import vn.cozynest.identity.domain.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private vn.cozynest.identity.domain.repository.LandlordProfileRepository landlordProfileRepository;
    @Mock
    private PasswordEncoderPort passwordEncoder;
    @Mock
    private TokenProviderPort tokenProvider;
    @Mock
    private vn.cozynest.identity.application.port.LoginAttemptPort loginAttemptPort;
    @Mock
    private vn.cozynest.identity.application.port.RateLimiterPort rateLimiter;
    @Mock
    private vn.cozynest.identity.application.service.TwoFactorAuthService twoFactorAuthService;

    private LoginUseCase loginUseCase;

    @BeforeEach
    void setUp() {
        loginUseCase = new LoginUseCase(userRepository, refreshTokenRepository, landlordProfileRepository, passwordEncoder, tokenProvider, loginAttemptPort, rateLimiter, twoFactorAuthService);
    }

    @Test
    @DisplayName("Login successfully with valid credentials")
    void loginSuccess() {
        User user = User.createNew("tenant@cozynest.vn", "0901234567", "hashed_password", "Nguyễn Văn Thuê", UserRoleType.TENANT);
        when(userRepository.findByEmail("tenant@cozynest.vn")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(tokenProvider.generateAccessToken(any(User.class))).thenReturn("mock_access_token");
        when(tokenProvider.generateRefreshToken()).thenReturn("mock_refresh_token");
        when(tokenProvider.hashToken("mock_refresh_token")).thenReturn("mock_token_hash");

        LoginCommand command = LoginCommand.builder()
                .email("tenant@cozynest.vn")
                .password("password123")
                .build();

        AuthResult result = loginUseCase.execute(command);

        assertNotNull(result);
        assertEquals("mock_access_token", result.getToken());
        assertEquals("mock_refresh_token", result.getRefreshToken());
        assertEquals("tenant@cozynest.vn", result.getUser().getEmail());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Login fails with invalid password")
    void loginInvalidPassword() {
        User user = User.createNew("tenant@cozynest.vn", "0901234567", "hashed_password", "Nguyễn Văn Thuê", UserRoleType.TENANT);
        when(userRepository.findByEmail("tenant@cozynest.vn")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

        LoginCommand command = LoginCommand.builder()
                .email("tenant@cozynest.vn")
                .password("wrong_password")
                .build();

        IdentityDomainException exception = assertThrows(IdentityDomainException.class, () -> loginUseCase.execute(command));
        assertEquals("ERR_AUTH_INVALID_CREDENTIALS", exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw exception when account is temporarily blocked after too many attempts")
    void loginBlockedAfterFailedAttempts() {
        when(loginAttemptPort.isBlocked(eq("blocked@cozynest.vn"), any())).thenReturn(true);

        LoginCommand command = LoginCommand.builder()
                .email("blocked@cozynest.vn")
                .password("anyPassword")
                .build();

        IdentityDomainException ex = assertThrows(IdentityDomainException.class, () -> loginUseCase.execute(command));
        assertEquals("ERR_AUTH_TEMPORARY_LOCKED", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should upgrade password hash when old cost encoding is detected")
    void shouldUpgradePasswordHashWhenOldCostDetected() {
        User user = User.createNew("tenant@cozynest.vn", "0901234567", "old_cost_hash", "Nguyễn Văn Thuê", UserRoleType.TENANT);
        when(userRepository.findByEmail("tenant@cozynest.vn")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "old_cost_hash")).thenReturn(true);
        when(passwordEncoder.upgradeEncoding("old_cost_hash")).thenReturn(true);
        when(passwordEncoder.encode("password123")).thenReturn("new_cost_hash");
        when(tokenProvider.generateAccessToken(any(User.class))).thenReturn("mock_token");
        when(tokenProvider.generateRefreshToken()).thenReturn("mock_refresh");
        when(tokenProvider.hashToken("mock_refresh")).thenReturn("mock_hash");

        LoginCommand command = LoginCommand.builder()
                .email("tenant@cozynest.vn")
                .password("password123")
                .build();

        AuthResult result = loginUseCase.execute(command);
        assertNotNull(result);
        assertEquals("new_cost_hash", user.getPasswordHash());
        verify(passwordEncoder).upgradeEncoding("old_cost_hash");
        verify(userRepository, atLeastOnce()).save(user);
    }

    @Test
    @DisplayName("Should reject password exceeding 72 bytes")
    void shouldRejectPasswordExceeding72Bytes() {
        LoginCommand command = LoginCommand.builder()
                .email("tenant@cozynest.vn")
                .password("A".repeat(73))
                .build();

        IdentityDomainException ex = assertThrows(IdentityDomainException.class, () -> loginUseCase.execute(command));
        assertEquals("ERR_AUTH_INVALID_CREDENTIALS", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should throw ERR_2FA_REQUIRED when Admin logs in without 2FA code")
    void shouldThrowErr2FaRequiredWhenAdminLogsInWithout2FaCode() {
        User admin = User.createNew("admin@cozynest.vn", "0903000003", "hashed_pwd", "Admin", UserRoleType.ADMIN);
        when(userRepository.findByEmail("admin@cozynest.vn")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("pwd123", "hashed_pwd")).thenReturn(true);

        LoginCommand command = LoginCommand.builder()
                .email("admin@cozynest.vn")
                .password("pwd123")
                .build();

        IdentityDomainException ex = assertThrows(IdentityDomainException.class, () -> loginUseCase.execute(command));
        assertEquals("ERR_2FA_REQUIRED", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should throw ERR_2FA_INVALID when Admin logs in with wrong 2FA code")
    void shouldThrowErr2FaInvalidWhenAdminLogsInWithWrong2FaCode() {
        User admin = User.createNew("admin@cozynest.vn", "0903000003", "hashed_pwd", "Admin", UserRoleType.ADMIN);
        when(userRepository.findByEmail("admin@cozynest.vn")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("pwd123", "hashed_pwd")).thenReturn(true);
        when(twoFactorAuthService.verifyChallenge(admin.getId(), "000000")).thenReturn(false);

        LoginCommand command = LoginCommand.builder()
                .email("admin@cozynest.vn")
                .password("pwd123")
                .twoFactorCode("000000")
                .build();

        IdentityDomainException ex = assertThrows(IdentityDomainException.class, () -> loginUseCase.execute(command));
        assertEquals("ERR_2FA_INVALID", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should login successfully when Admin provides valid 2FA code")
    void shouldSucceedWhenAdminProvidesValid2FaCode() {
        User admin = User.createNew("admin@cozynest.vn", "0903000003", "hashed_pwd", "Admin", UserRoleType.ADMIN);
        when(userRepository.findByEmail("admin@cozynest.vn")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("pwd123", "hashed_pwd")).thenReturn(true);
        when(twoFactorAuthService.verifyChallenge(admin.getId(), "123456")).thenReturn(true);
        when(tokenProvider.generateAccessToken(admin)).thenReturn("admin_token");
        when(tokenProvider.generateRefreshToken()).thenReturn("admin_refresh");
        when(tokenProvider.hashToken("admin_refresh")).thenReturn("hash");

        LoginCommand command = LoginCommand.builder()
                .email("admin@cozynest.vn")
                .password("pwd123")
                .twoFactorCode("123456")
                .build();

        AuthResult result = loginUseCase.execute(command);
        assertNotNull(result);
        assertEquals("admin_token", result.getToken());
    }

    @Test
    @DisplayName("Should throw ERR_2FA_REQUIRED when Moderator logs in without 2FA code")
    void shouldThrowErr2FaRequiredWhenModeratorLogsInWithout2FaCode() {
        User mod = User.createNew("mod@cozynest.vn", "0904000004", "hashed_pwd", "Moderator", UserRoleType.MODERATOR);
        when(userRepository.findByEmail("mod@cozynest.vn")).thenReturn(Optional.of(mod));
        when(passwordEncoder.matches("pwd123", "hashed_pwd")).thenReturn(true);

        LoginCommand command = LoginCommand.builder()
                .email("mod@cozynest.vn")
                .password("pwd123")
                .build();

        IdentityDomainException ex = assertThrows(IdentityDomainException.class, () -> loginUseCase.execute(command));
        assertEquals("ERR_2FA_REQUIRED", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should throw ERR_2FA_REQUIRED when regular user has 2FA enabled but no code provided")
    void shouldThrowErr2FaRequiredWhenUserHasTwoFactorEnabled() {
        User user = User.createNew("user2fa@cozynest.vn", "0905000005", "hashed_pwd", "User 2FA", UserRoleType.LANDLORD);
        user.enableTwoFactor();
        when(userRepository.findByEmail("user2fa@cozynest.vn")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pwd123", "hashed_pwd")).thenReturn(true);

        LoginCommand command = LoginCommand.builder()
                .email("user2fa@cozynest.vn")
                .password("pwd123")
                .build();

        IdentityDomainException ex = assertThrows(IdentityDomainException.class, () -> loginUseCase.execute(command));
        assertEquals("ERR_2FA_REQUIRED", ex.getErrorCode());
    }
}

