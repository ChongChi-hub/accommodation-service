package vn.cozynest.identity.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import vn.cozynest.identity.application.port.SmsSenderPort;
import vn.cozynest.identity.domain.model.PhoneVerification;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.model.UserRoleType;
import vn.cozynest.identity.domain.repository.PhoneVerificationRepository;
import vn.cozynest.identity.domain.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhoneVerificationServiceTest {

    @Mock
    private PhoneVerificationRepository verificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SmsSenderPort smsSenderPort;
    @Mock
    private Environment environment;

    @Test
    void rejectsDisposableAndVirtualVoipNumbers() {
        PhoneVerificationService service = new PhoneVerificationService(
                verificationRepository, userRepository, smsSenderPort, false, environment);

        assertThatThrownBy(() -> service.sendOtp("0999123456"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.sendOtp("0900000000"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(smsSenderPort);
    }

    @Test
    void blocksFixedSandboxOtpInProductionProfile() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(verificationRepository.countRecentByPhone(eq("0912345678"), anyLong())).thenReturn(0L);
        when(verificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PhoneVerificationService service = new PhoneVerificationService(
                verificationRepository, userRepository, smsSenderPort, true, environment);

        String masked = service.sendOtp("+84912345678");
        assertThat(masked).isEqualTo("091****678");

        ArgumentCaptor<PhoneVerification> savedCaptor = ArgumentCaptor.forClass(PhoneVerification.class);
        verify(verificationRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getPhoneNumber()).isEqualTo("0912345678");
    }

    @Test
    void enforcesDailySmsQuota() {
        when(verificationRepository.countRecentByPhone("0987654321", 600)).thenReturn(0L);
        when(verificationRepository.countRecentByPhone("0987654321", 86_400)).thenReturn(5L);

        PhoneVerificationService service = new PhoneVerificationService(
                verificationRepository, userRepository, smsSenderPort, false, environment);

        assertThatThrownBy(() -> service.sendOtp("0987654321"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void requiresOldOtpWhenChangingAlreadyVerifiedPhone() {
        User user = User.createNew("user@example.com", "0901112233", "hash", "Test User", UserRoleType.TENANT);
        user.verifyPhoneNow();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        PhoneVerificationService service = new PhoneVerificationService(
                verificationRepository, userRepository, smsSenderPort, false, environment);

        assertThatThrownBy(() -> service.changePhoneNumber(user.getId(), "0902223344", "111222", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changesPhoneSuccessfullyWhenOldAndNewOtpVerify() {
        User user = User.createNew("user@example.com", "0901112233", "hash", "Test User", UserRoleType.TENANT);
        user.verifyPhoneNow();

        PhoneVerification oldV = PhoneVerification.create("0901112233", "111111", 300);
        PhoneVerification newV = PhoneVerification.create("0902223344", "222222", 300);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(verificationRepository.findLatestByPhone("0901112233")).thenReturn(Optional.of(oldV));
        when(verificationRepository.findLatestByPhone("0902223344")).thenReturn(Optional.of(newV));

        PhoneVerificationService service = new PhoneVerificationService(
                verificationRepository, userRepository, smsSenderPort, false, environment);

        String masked = service.changePhoneNumber(user.getId(), "+84902223344", "222222", "111111");
        assertThat(masked).isEqualTo("090****344");
        assertThat(user.getPhone()).isEqualTo("0902223344");
        verify(userRepository).save(user);
    }
}

