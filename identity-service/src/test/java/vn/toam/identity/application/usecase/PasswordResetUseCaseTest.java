package vn.toam.identity.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.toam.identity.application.dto.ForgotPasswordCommand;
import vn.toam.identity.application.dto.ResetPasswordCommand;
import vn.toam.identity.application.port.EmailServicePort;
import vn.toam.identity.application.port.PasswordEncoderPort;
import vn.toam.identity.domain.exception.IdentityDomainException;
import vn.toam.identity.domain.model.User;
import vn.toam.identity.domain.model.UserRoleType;
import vn.toam.identity.domain.repository.RefreshTokenRepository;
import vn.toam.identity.domain.repository.UserRepository;
import vn.toam.identity.infrastructure.persistence.entity.PasswordResetTokenJpaEntity;
import vn.toam.identity.infrastructure.persistence.repository.SpringDataPasswordResetTokenRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetUseCaseTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SpringDataPasswordResetTokenRepository resetTokenRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoderPort passwordEncoder;
    @Mock
    private EmailServicePort emailService;

    private ForgotPasswordUseCase forgotPasswordUseCase;
    private ResetPasswordUseCase resetPasswordUseCase;

    @BeforeEach
    void setUp() {
        forgotPasswordUseCase = new ForgotPasswordUseCase(userRepository, resetTokenRepository, passwordEncoder, emailService);
        resetPasswordUseCase = new ResetPasswordUseCase(userRepository, resetTokenRepository, refreshTokenRepository, passwordEncoder, emailService);
    }

    @Test
    @DisplayName("Forgot password should generate OTP and send email when user exists")
    void forgotPasswordSuccess() {
        User user = User.createNew("tenant@toam.vn", "0901234567", "hash", "Nguyen Van Thue", UserRoleType.TENANT);
        when(userRepository.findByEmail("tenant@toam.vn")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_otp");

        ForgotPasswordCommand command = ForgotPasswordCommand.builder()
                .email("tenant@toam.vn")
                .ipAddress("127.0.0.1")
                .build();

        assertDoesNotThrow(() -> forgotPasswordUseCase.execute(command));
        verify(resetTokenRepository).invalidateAllActiveTokens(user.getId());
        verify(resetTokenRepository).save(any(PasswordResetTokenJpaEntity.class));
        verify(emailService).sendPasswordResetOtp(eq("tenant@toam.vn"), eq("Nguyen Van Thue"), anyString());
    }

    @Test
    @DisplayName("Forgot password should not reveal if email does not exist")
    void forgotPasswordNonExistentEmail() {
        when(userRepository.findByEmail("unknown@toam.vn")).thenReturn(Optional.empty());

        ForgotPasswordCommand command = ForgotPasswordCommand.builder()
                .email("unknown@toam.vn")
                .build();

        assertDoesNotThrow(() -> forgotPasswordUseCase.execute(command));
        verify(resetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetOtp(any(), any(), any());
    }

    @Test
    @DisplayName("Reset password success with valid OTP and revokes all refresh tokens")
    void resetPasswordSuccess() {
        User user = User.createNew("tenant@toam.vn", "0901234567", "old_hash", "Nguyen Van Thue", UserRoleType.TENANT);
        when(userRepository.findByEmail("tenant@toam.vn")).thenReturn(Optional.of(user));

        PasswordResetTokenJpaEntity token = PasswordResetTokenJpaEntity.builder()
                .id("tok-1")
                .userId(user.getId())
                .otpHash("hashed_otp")
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .attempts(0)
                .build();

        when(resetTokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.matches("123456", "hashed_otp")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("new_hash");

        ResetPasswordCommand command = ResetPasswordCommand.builder()
                .email("tenant@toam.vn")
                .otp("123456")
                .newPassword("newPassword123")
                .build();

        assertDoesNotThrow(() -> resetPasswordUseCase.execute(command));
        assertTrue(token.isUsed());
        assertEquals("new_hash", user.getPasswordHash());
        verify(refreshTokenRepository).revokeAllByUserId(user.getId());
        verify(emailService).sendPasswordChangedNotice(eq("tenant@toam.vn"), eq("Nguyen Van Thue"));
    }

    @Test
    @DisplayName("Reset password fails and increments attempt when OTP is incorrect")
    void resetPasswordIncorrectOtp() {
        User user = User.createNew("tenant@toam.vn", "0901234567", "old_hash", "Nguyen Van Thue", UserRoleType.TENANT);
        when(userRepository.findByEmail("tenant@toam.vn")).thenReturn(Optional.of(user));

        PasswordResetTokenJpaEntity token = PasswordResetTokenJpaEntity.builder()
                .id("tok-1")
                .userId(user.getId())
                .otpHash("hashed_otp")
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .attempts(1)
                .build();

        when(resetTokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.matches("999999", "hashed_otp")).thenReturn(false);

        ResetPasswordCommand command = ResetPasswordCommand.builder()
                .email("tenant@toam.vn")
                .otp("999999")
                .newPassword("newPassword123")
                .build();

        IdentityDomainException ex = assertThrows(IdentityDomainException.class, () -> resetPasswordUseCase.execute(command));
        assertEquals("ERR_AUTH_INVALID_OTP", ex.getErrorCode());
        assertEquals(2, token.getAttempts());
        verify(resetTokenRepository).save(token);
    }
}
