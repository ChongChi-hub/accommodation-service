package vn.cozynest.identity.application.service;

import org.junit.jupiter.api.Test;
import vn.cozynest.identity.domain.model.TwoFactorRecoveryRequest;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.model.UserRoleType;
import vn.cozynest.identity.domain.model.UserTwoFactor;
import vn.cozynest.identity.domain.repository.TwoFactorRecoveryRequestRepository;
import vn.cozynest.identity.domain.repository.UserRepository;
import vn.cozynest.identity.domain.repository.UserTwoFactorRepository;
import vn.cozynest.identity.infrastructure.security.TotpHelper;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TwoFactorHardeningTest {

    @Test
    void shouldLockAfter5FailedAttemptsAndConsumeBackupCodeSingleUse() {
        UserTwoFactorRepository tfRepo = mock(UserTwoFactorRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        TotpHelper totp = mock(TotpHelper.class);
        TwoFactorRecoveryRequestRepository recRepo = mock(TwoFactorRecoveryRequestRepository.class);
        TwoFactorAuthService service = new TwoFactorAuthService(tfRepo, userRepo, totp, recRepo);

        UserTwoFactor tf = new UserTwoFactor("u1", "SECRET", true, 0, null,
                "[\"11112222\",\"33334444\"]", Instant.now(), Instant.now());
        when(tfRepo.findByUserId("u1")).thenReturn(Optional.of(tf));
        when(tfRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(totp.verifyCode(any(), any())).thenReturn(false);

        assertThat(service.verifyChallenge("u1", "11112222")).isTrue();
        assertThat(service.verifyChallenge("u1", "11112222")).isFalse(); // Single-use consumed!

        for (int i = 0; i < 4; i++) {
            assertThat(service.verifyChallenge("u1", "999999")).isFalse();
        }
        assertThatThrownBy(() -> service.verifyChallenge("u1", "33334444"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("15 phut");
    }

    @Test
    void shouldEnforceTwoAdminRuleOnRecovery() {
        UserTwoFactorRepository tfRepo = mock(UserTwoFactorRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        TotpHelper totp = mock(TotpHelper.class);
        TwoFactorRecoveryRequestRepository recRepo = mock(TwoFactorRecoveryRequestRepository.class);
        TwoFactorAuthService service = new TwoFactorAuthService(tfRepo, userRepo, totp, recRepo);

        TwoFactorRecoveryRequest req = TwoFactorRecoveryRequest.request("u1", "admin-1", "Lost device");
        when(recRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(recRepo.findById(req.getId())).thenReturn(Optional.of(req));

        UserTwoFactor tf = new UserTwoFactor("u1", "SECRET", true, "[]", Instant.now(), Instant.now());
        User user = User.createNew("u1@test.vn", "0901234567", "hash", "Test", UserRoleType.TENANT);
        user.enableTwoFactor();
        when(tfRepo.findByUserId("u1")).thenReturn(Optional.of(tf));
        when(userRepo.findById("u1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.approveAdminRecovery(req.getId(), "admin-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("2 quan tri vien");

        TwoFactorRecoveryRequest approved = service.approveAdminRecovery(req.getId(), "admin-2");
        assertThat(approved.getStatus()).isEqualTo("APPROVED");
        assertThat(tf.isEnabled()).isFalse();
        assertThat(user.isTwoFactorEnabled()).isFalse();
    }
}

