package vn.cozynest.identity.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.cozynest.identity.application.dto.VerifyEmailCommand;
import vn.cozynest.identity.application.port.EmailServicePort;
import vn.cozynest.identity.application.port.PasswordEncoderPort;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.model.UserRoleType;
import vn.cozynest.identity.domain.repository.UserRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.EmailVerificationTokenJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataEmailVerificationTokenRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerifyEmailUseCaseTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SpringDataEmailVerificationTokenRepository tokenRepository;
    @Mock
    private PasswordEncoderPort passwordEncoder;
    @Mock
    private EmailServicePort emailService;

    private VerifyEmailUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new VerifyEmailUseCase(userRepository, tokenRepository, passwordEncoder, emailService);
    }

    @Test
    @DisplayName("sendVerificationOtp performs dummy encode and returns silently for unknown email (anti-enumeration)")
    void shouldProtectUnknownEmailEnumeration() {
        when(userRepository.findByEmail("unknown@cozynest.vn")).thenReturn(Optional.empty());

        useCase.sendVerificationOtp("UNKNOWN@cozynest.vn");

        verify(passwordEncoder).encode("000000");
        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendEmailVerificationOtp(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("sendVerificationOtp enforces 60s cooldown rate limit")
    void shouldEnforce60SecondCooldown() {
        User user = User.createNew("u@cozynest.vn", "0901112223", "hash", "Test User", UserRoleType.TENANT);
        when(userRepository.findByEmail("u@cozynest.vn")).thenReturn(Optional.of(user));
        when(tokenRepository.findLatestActiveTokenByUserId(user.getId())).thenReturn(Optional.of(
                EmailVerificationTokenJpaEntity.builder()
                        .id("tok-1")
                        .userId(user.getId())
                        .tokenHash("hashed")
                        .createdAt(Instant.now().minus(15, ChronoUnit.SECONDS))
                        .expiresAt(Instant.now().plus(9, ChronoUnit.MINUTES))
                        .build()
        ));

        assertThatThrownBy(() -> useCase.sendVerificationOtp("u@cozynest.vn"))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_RATE_LIMIT_EXCEEDED");
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("sendVerificationOtp invalidates previous OTPs and sets 10-minute expiry")
    void shouldInvalidateOldOtpsAndSet10MinExpiry() {
        User user = User.createNew("u@cozynest.vn", "0901112223", "hash", "Test User", UserRoleType.TENANT);
        when(userRepository.findByEmail("u@cozynest.vn")).thenReturn(Optional.of(user));
        when(tokenRepository.findLatestActiveTokenByUserId(user.getId())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("otp-hash");

        useCase.sendVerificationOtp("u@cozynest.vn");

        verify(tokenRepository).invalidateAllActiveTokens(user.getId());
        ArgumentCaptor<EmailVerificationTokenJpaEntity> captor = ArgumentCaptor.forClass(EmailVerificationTokenJpaEntity.class);
        verify(tokenRepository).save(captor.capture());
        EmailVerificationTokenJpaEntity saved = captor.getValue();
        long ttlSeconds = ChronoUnit.SECONDS.between(saved.getCreatedAt(), saved.getExpiresAt());
        assertThat(ttlSeconds).isBetween(595L, 605L);
        verify(emailService).sendEmailVerificationOtp(eq("u@cozynest.vn"), eq("Test User"), anyString());
    }

    @Test
    @DisplayName("verifyEmail rejects expired OTP with ERR_AUTH_OTP_EXPIRED")
    void shouldRejectExpiredOtp() {
        User user = User.createNew("u@cozynest.vn", "0901112223", "hash", "Test User", UserRoleType.TENANT);
        when(userRepository.findByEmail("u@cozynest.vn")).thenReturn(Optional.of(user));
        when(tokenRepository.findLatestActiveTokenByUserId(user.getId())).thenReturn(Optional.of(
                EmailVerificationTokenJpaEntity.builder()
                        .id("tok-exp")
                        .userId(user.getId())
                        .tokenHash("otp-hash")
                        .createdAt(Instant.now().minus(12, ChronoUnit.MINUTES))
                        .expiresAt(Instant.now().minus(2, ChronoUnit.MINUTES))
                        .build()
        ));

        assertThatThrownBy(() -> useCase.verifyEmail(VerifyEmailCommand.builder().email("u@cozynest.vn").otp("123456").build()))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_AUTH_OTP_EXPIRED");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("verifyEmail rejects wrong OTP with ERR_AUTH_INVALID_OTP and succeeds with valid OTP")
    void shouldRejectWrongOtpAndVerifyValidOtp() {
        User user = User.createNew("u@cozynest.vn", "0901112223", "hash", "Test User", UserRoleType.TENANT);
        EmailVerificationTokenJpaEntity token = EmailVerificationTokenJpaEntity.builder()
                .id("tok-ok")
                .userId(user.getId())
                .tokenHash("otp-hash")
                .createdAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .expiresAt(Instant.now().plus(9, ChronoUnit.MINUTES))
                .build();

        when(userRepository.findByEmail("u@cozynest.vn")).thenReturn(Optional.of(user));
        when(tokenRepository.findLatestActiveTokenByUserId(user.getId())).thenReturn(Optional.of(token));
        when(passwordEncoder.matches("000000", "otp-hash")).thenReturn(false);
        when(passwordEncoder.matches("654321", "otp-hash")).thenReturn(true);

        assertThatThrownBy(() -> useCase.verifyEmail(VerifyEmailCommand.builder().email("u@cozynest.vn").otp("000000").build()))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_AUTH_INVALID_OTP");

        useCase.verifyEmail(VerifyEmailCommand.builder().email("u@cozynest.vn").otp("654321").build());

        assertThat(token.getVerifiedAt()).isNotNull();
        assertThat(user.getEmailVerifiedAt()).isNotNull();
        verify(tokenRepository).save(token);
        verify(userRepository).save(user);
    }
}

