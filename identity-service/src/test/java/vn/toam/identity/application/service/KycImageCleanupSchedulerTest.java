package vn.toam.identity.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.toam.identity.domain.model.KycRejectReasonCode;
import vn.toam.identity.domain.model.KycStatus;
import vn.toam.identity.domain.model.UserVerification;
import vn.toam.identity.domain.port.KycPrivateStoragePort;
import vn.toam.identity.domain.repository.UserVerificationRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KycImageCleanupSchedulerTest {

    @Test
    @DisplayName("Scheduler is disabled by default: does not query or delete any files")
    void testSchedulerDisabledByDefault() {
        UserVerificationRepository repo = mock(UserVerificationRepository.class);
        KycPrivateStoragePort storage = mock(KycPrivateStoragePort.class);

        // cleanupEnabled is false by default
        KycImageCleanupScheduler scheduler = new KycImageCleanupScheduler(repo, storage, false, 30);
        scheduler.cleanupRejectedKycImages();

        verifyNoInteractions(repo);
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("Scheduler only purges REJECTED verifications and never purges PENDING or APPROVED")
    void testSchedulerPurgesOnlyRejected() {
        UserVerificationRepository repo = mock(UserVerificationRepository.class);
        KycPrivateStoragePort storage = mock(KycPrivateStoragePort.class);

        UserVerification rejected = new UserVerification(
                "v-rej-01", "u-landlord-01", "Nguyen Van A", LocalDate.of(1990, 1, 1),
                "enc", "hash", "1234", LocalDate.of(2020, 1, 1), "CA TP HCM",
                "kyc/private/u-landlord-01_front.webp", "kyc/private/u-landlord-01_back.webp", null,
                KycStatus.REJECTED, Instant.now().minusSeconds(86400 * 35), "v1.0",
                Instant.now().minusSeconds(86400 * 35), Instant.now().minusSeconds(86400 * 32),
                "u-mod-01", KycRejectReasonCode.BLURRY, "Image is blurry", 1, 1L,
                Instant.now().minusSeconds(86400 * 35), Instant.now().minusSeconds(86400 * 32)
        );

        when(repo.findRejectedBefore(any(Instant.class))).thenReturn(List.of(rejected));

        KycImageCleanupScheduler scheduler = new KycImageCleanupScheduler(repo, storage, true, 30);
        scheduler.cleanupRejectedKycImages();

        // Verifies physical storage delete called for the 2 keys
        verify(storage, times(1)).delete("kyc/private/u-landlord-01_front.webp");
        verify(storage, times(1)).delete("kyc/private/u-landlord-01_back.webp");

        // Verifies database status marked as PURGED
        verify(repo, times(1)).save(argThat(v ->
                "PURGED".equals(v.getFrontImageKey()) &&
                "PURGED".equals(v.getBackImageKey()) &&
                v.getStatus() == KycStatus.REJECTED
        ));
    }
}
