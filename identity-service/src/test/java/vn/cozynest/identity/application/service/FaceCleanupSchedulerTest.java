package vn.cozynest.identity.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.cozynest.identity.domain.model.FaceVerification;
import vn.cozynest.identity.domain.model.FaceVerificationFrame;
import vn.cozynest.identity.domain.model.FaceVerificationStatus;
import vn.cozynest.identity.domain.port.FacePrivateStoragePort;
import vn.cozynest.identity.domain.repository.FaceVerificationFrameRepository;
import vn.cozynest.identity.domain.repository.FaceVerificationRepository;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FaceCleanupSchedulerTest {

    @Test
    @DisplayName("FaceCleanupScheduler does nothing when face.cleanup.enabled=false")
    void shouldDoNothingWhenDisabled() {
        FaceVerificationRepository faceRepo = mock(FaceVerificationRepository.class);
        FaceVerificationFrameRepository frameRepo = mock(FaceVerificationFrameRepository.class);
        FacePrivateStoragePort storage = mock(FacePrivateStoragePort.class);

        FaceCleanupScheduler scheduler = new FaceCleanupScheduler(faceRepo, frameRepo, storage, false, 30);
        scheduler.cleanupExpiredFaceFrames();

        verifyNoInteractions(faceRepo, frameRepo, storage);
    }

    @Test
    @DisplayName("FaceCleanupScheduler only queries PASSED and FAILED decided records and deletes encrypted frames")
    void shouldPurgeOnlyEligibleDecidedFrames() {
        FaceVerificationRepository faceRepo = mock(FaceVerificationRepository.class);
        FaceVerificationFrameRepository frameRepo = mock(FaceVerificationFrameRepository.class);
        FacePrivateStoragePort storage = mock(FacePrivateStoragePort.class);

        FaceVerification fv = FaceVerification.createChallenge(
                "u-1", "kyc-1", "manual", "[]", "nonce", Instant.now().minusSeconds(86400 * 40), 1, "v1.0"
        );
        fv.pass("mod-1");
        FaceVerificationFrame frame = FaceVerificationFrame.create(fv.getId(), "NEUTRAL", "face/u-1/f1.enc", "sha", 128);

        when(faceRepo.findEligibleForPurge(any(Instant.class), eq(List.of(FaceVerificationStatus.PASSED, FaceVerificationStatus.FAILED))))
                .thenReturn(List.of(fv));
        when(frameRepo.findActiveByFaceVerificationId(fv.getId())).thenReturn(List.of(frame));

        FaceCleanupScheduler scheduler = new FaceCleanupScheduler(faceRepo, frameRepo, storage, true, 30);
        scheduler.cleanupExpiredFaceFrames();

        verify(storage).delete("face/u-1/f1.enc");
        verify(frameRepo).save(argThat(FaceVerificationFrame::isDeleted));
    }
}

