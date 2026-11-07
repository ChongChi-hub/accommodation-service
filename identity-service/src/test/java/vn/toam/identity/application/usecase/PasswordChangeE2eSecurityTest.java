package vn.toam.identity.application.usecase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.toam.identity.application.dto.ChangePasswordCommand;
import vn.toam.identity.application.port.EmailServicePort;
import vn.toam.identity.application.port.PasswordEncoderPort;
import vn.toam.identity.domain.exception.IdentityDomainException;
import vn.toam.identity.domain.model.RefreshToken;
import vn.toam.identity.domain.model.User;
import vn.toam.identity.domain.model.UserRoleType;
import vn.toam.identity.domain.repository.RefreshTokenRepository;
import vn.toam.identity.domain.repository.UserRepository;
import vn.toam.identity.infrastructure.security.RedisRateLimiterAdapter;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PasswordChangeE2eSecurityTest {

    static class InMemoryRefreshTokenRepository implements RefreshTokenRepository {
        final Map<String, RefreshToken> store = new HashMap<>();

        @Override
        public RefreshToken save(RefreshToken token) {
            store.put(token.getTokenHash(), token);
            return token;
        }

        @Override
        public Optional<RefreshToken> findByTokenHash(String tokenHash) {
            return Optional.ofNullable(store.get(tokenHash));
        }

        @Override
        public void revokeAllByUserId(String userId) {
            store.values().stream().filter(t -> t.getUserId().equals(userId)).forEach(t -> t.revoke(null));
        }

        @Override
        public void revokeAllExceptByUserId(String userId, String keepTokenHash) {
            store.values().stream()
                    .filter(t -> t.getUserId().equals(userId) && !t.getTokenHash().equals(keepTokenHash))
                    .forEach(t -> t.revoke(null));
        }
    }

    @Test
    @DisplayName("8. Password Change: Revokes other sessions while keeping current session active")
    void testPasswordChangeTwoSessions() {
        UserRepository userRepo = mock(UserRepository.class);
        PasswordEncoderPort passwordEncoder = mock(PasswordEncoderPort.class);
        EmailServicePort emailService = mock(EmailServicePort.class);
        InMemoryRefreshTokenRepository tokenRepo = new InMemoryRefreshTokenRepository();

        User user = User.createNew("landlord@toam.vn", "0901234567", "encodedOldPassword", "Nguyen Landlord", UserRoleType.LANDLORD);
        when(userRepo.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword@123", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.matches("NewStrongPassword@2026", "encodedOldPassword")).thenReturn(false);
        when(passwordEncoder.encode("NewStrongPassword@2026")).thenReturn("encodedNewPassword");

        // Two active sessions
        RefreshToken sessionA = new RefreshToken("tok-a", user.getId(), "hash-session-a", "Chrome/Win", "1.1.1.1", Instant.now().plus(Duration.ofDays(30)), null, null, Instant.now());
        RefreshToken sessionB = new RefreshToken("tok-b", user.getId(), "hash-session-b", "Safari/iOS", "2.2.2.2", Instant.now().plus(Duration.ofDays(30)), null, null, Instant.now());
        tokenRepo.save(sessionA);
        tokenRepo.save(sessionB);

        RedisRateLimiterAdapter rateLimiter = new RedisRateLimiterAdapter(null, null);
        ChangePasswordUseCase useCase = new ChangePasswordUseCase(userRepo, passwordEncoder, tokenRepo, emailService, rateLimiter);

        ChangePasswordCommand cmd = new ChangePasswordCommand("OldPassword@123", "NewStrongPassword@2026", "hash-session-a", "1.1.1.1");
        useCase.execute(user.getId(), cmd);

        // Assert Session A remains active (isValid == true)
        assertThat(tokenRepo.findByTokenHash("hash-session-a").orElseThrow().isValid()).isTrue();

        // Assert Session B is revoked (isRevoked == true -> triggers 401 on refresh)
        assertThat(tokenRepo.findByTokenHash("hash-session-b").orElseThrow().isRevoked()).isTrue();
        assertThat(tokenRepo.findByTokenHash("hash-session-b").orElseThrow().getRevokedAt()).isNotNull();

        // Assert email notification was sent
        verify(emailService, times(1)).sendPasswordChangedNotice(eq("landlord@toam.vn"), eq("Nguyen Landlord"));
    }

    @Test
    @DisplayName("8. 5 consecutive wrong password attempts triggers 15-minute lock without sleeping")
    void testFiveFailedPasswordAttemptsLocksFor15Minutes() {
        UserRepository userRepo = mock(UserRepository.class);
        PasswordEncoderPort passwordEncoder = mock(PasswordEncoderPort.class);
        EmailServicePort emailService = mock(EmailServicePort.class);
        RefreshTokenRepository tokenRepo = mock(RefreshTokenRepository.class);

        User user = User.createNew("landlord@toam.vn", "0901234567", "encodedOldPassword", "Nguyen Landlord", UserRoleType.LANDLORD);
        when(userRepo.findById("u-landlord-01")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "encodedOldPassword")).thenReturn(false);

        RedisRateLimiterAdapter rateLimiter = new RedisRateLimiterAdapter(null, null);
        ChangePasswordUseCase useCase = new ChangePasswordUseCase(userRepo, passwordEncoder, tokenRepo, emailService, rateLimiter);

        ChangePasswordCommand cmd = new ChangePasswordCommand("WrongPassword", "NewStrongPassword@2026", "hash-a", "1.1.1.1");

        // 5 consecutive failures
        for (int i = 1; i <= 5; i++) {
            assertThrows(IdentityDomainException.class, () -> useCase.execute("u-landlord-01", cmd));
        }

        // 6th attempt is locked: throws ERR_AUTH_PASSWORD_CHANGE_LOCKED
        IdentityDomainException lockedEx = assertThrows(IdentityDomainException.class, () ->
                useCase.execute("u-landlord-01", cmd)
        );
        assertThat(lockedEx.getErrorCode()).isEqualTo("ERR_AUTH_PASSWORD_CHANGE_LOCKED");
    }
}
