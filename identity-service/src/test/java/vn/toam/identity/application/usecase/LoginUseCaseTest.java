package vn.toam.identity.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.toam.identity.application.dto.AuthResult;
import vn.toam.identity.application.dto.LoginCommand;
import vn.toam.identity.application.port.PasswordEncoderPort;
import vn.toam.identity.application.port.TokenProviderPort;
import vn.toam.identity.domain.exception.IdentityDomainException;
import vn.toam.identity.domain.model.User;
import vn.toam.identity.domain.model.UserRoleType;
import vn.toam.identity.domain.repository.RefreshTokenRepository;
import vn.toam.identity.domain.repository.UserRepository;

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
    private PasswordEncoderPort passwordEncoder;
    @Mock
    private TokenProviderPort tokenProvider;

    private LoginUseCase loginUseCase;

    @BeforeEach
    void setUp() {
        loginUseCase = new LoginUseCase(userRepository, refreshTokenRepository, passwordEncoder, tokenProvider);
    }

    @Test
    @DisplayName("Login successfully with valid credentials")
    void loginSuccess() {
        User user = User.createNew("tenant@toam.vn", "0901234567", "hashed_password", "Nguyễn Văn Thuê", UserRoleType.TENANT);
        when(userRepository.findByEmail("tenant@toam.vn")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(tokenProvider.generateAccessToken(any(User.class))).thenReturn("mock_access_token");
        when(tokenProvider.generateRefreshToken()).thenReturn("mock_refresh_token");
        when(tokenProvider.hashToken("mock_refresh_token")).thenReturn("mock_token_hash");

        LoginCommand command = LoginCommand.builder()
                .email("tenant@toam.vn")
                .password("password123")
                .build();

        AuthResult result = loginUseCase.execute(command);

        assertNotNull(result);
        assertEquals("mock_access_token", result.getToken());
        assertEquals("mock_refresh_token", result.getRefreshToken());
        assertEquals("tenant@toam.vn", result.getUser().getEmail());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Login fails with invalid password")
    void loginInvalidPassword() {
        User user = User.createNew("tenant@toam.vn", "0901234567", "hashed_password", "Nguyễn Văn Thuê", UserRoleType.TENANT);
        when(userRepository.findByEmail("tenant@toam.vn")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

        LoginCommand command = LoginCommand.builder()
                .email("tenant@toam.vn")
                .password("wrong_password")
                .build();

        IdentityDomainException exception = assertThrows(IdentityDomainException.class, () -> loginUseCase.execute(command));
        assertEquals("ERR_AUTH_INVALID_CREDENTIALS", exception.getErrorCode());
    }
}

