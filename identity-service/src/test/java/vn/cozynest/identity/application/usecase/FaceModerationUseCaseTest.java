package vn.cozynest.identity.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.cozynest.identity.application.dto.FaceDecisionRequest;
import vn.cozynest.identity.application.dto.FaceModerationDetailDto;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.*;
import vn.cozynest.identity.domain.port.FacePrivateStoragePort;
import vn.cozynest.identity.domain.repository.*;
import vn.cozynest.identity.infrastructure.security.FaceCryptoService;
import vn.cozynest.identity.infrastructure.security.KycCryptoService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaceModerationUseCaseTest {

    @Mock
    private FaceVerificationRepository faceRepository;
    @Mock
    private FaceVerificationFrameRepository frameRepository;
    @Mock
    private FaceAccessLogRepository accessLogRepository;
    @Mock
    private FaceEventOutboxRepository outboxRepository;
    @Mock
    private UserVerificationRepository userVerificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FacePrivateStoragePort storagePort;
    @Mock
    private FaceCryptoService faceCryptoService;
    @Mock
    private KycCryptoService kycCryptoService;

    private FaceModerationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FaceModerationUseCase(
                faceRepository, frameRepository, accessLogRepository, outboxRepository,
                userVerificationRepository, userRepository, storagePort, faceCryptoService, kycCryptoService
        );
    }

    @Test
    @DisplayName("Moderation: Self-review of own face verification is strictly forbidden (ERR_SELF_REVIEW_FORBIDDEN)")
    void testSelfReviewForbidden() {
        FaceVerification fv = FaceVerification.createChallenge(
                "mod-user-1", "kyc-1", "manual", "[\"CHOP_MAT\"]", "nonce", Instant.now(), 1, "v1.0"
        );
        when(faceRepository.findByKycId("kyc-1")).thenReturn(Optional.of(fv));

        assertThatThrownBy(() -> useCase.getFaceDetail("mod-user-1", "MODERATOR", "kyc-1", "127.0.0.1"))
                .isInstanceOf(IdentityDomainException.class)
                .hasMessageContaining("không được phép tự duyệt");
    }

    @Test
    @DisplayName("Stream Frame: Stream writes exactly 1 face_access_logs entry")
    void testStreamFrameWritesAuditLog() {
        FaceVerification fv = FaceVerification.createChallenge(
                "user-1", "kyc-1", "manual", "[\"CHOP_MAT\"]", "nonce", Instant.now(), 1, "v1.0"
        );
        FaceVerificationFrame frame = FaceVerificationFrame.create(fv.getId(), "NEUTRAL", "key1", "hash", 100);

        when(faceRepository.findByKycId("kyc-1")).thenReturn(Optional.of(fv));
        when(frameRepository.findById("frame-1")).thenReturn(Optional.of(frame));
        when(storagePort.load("key1")).thenReturn(new byte[]{1, 2, 3});
        when(faceCryptoService.decrypt(any())).thenReturn(new byte[]{4, 5, 6});

        byte[] result = useCase.streamFrame("mod-1", "MODERATOR", "kyc-1", "frame-1", "127.0.0.1", "Agent/1.0");

        assertThat(result).containsExactly(4, 5, 6);
        verify(accessLogRepository, times(1)).save(argThat(log ->
                "STREAM_FRAME".equals(log.getPurpose()) && "mod-1".equals(log.getActorId())
        ));
    }

    @Test
    @DisplayName("Decide: Fails with ERR_FACE_VERSION_CONFLICT when version mismatches")
    void testOptimisticLockConflict() {
        FaceVerification fv = FaceVerification.createChallenge(
                "user-1", "kyc-1", "manual", "[\"CHOP_MAT\"]", "nonce", Instant.now(), 1, "v1.0"
        );
        fv.setVersion(2L);
        when(faceRepository.findByKycId("kyc-1")).thenReturn(Optional.of(fv));

        FaceDecisionRequest req = new FaceDecisionRequest();
        req.setDecision("PASSED");

        assertThatThrownBy(() -> useCase.decide("mod-1", "MODERATOR", "kyc-1", req, 1L, "127.0.0.1"))
                .isInstanceOf(IdentityDomainException.class)
                .hasMessageContaining("được cập nhật bởi kiểm duyệt viên khác");
    }

    @Test
    @DisplayName("Decide: Approves successfully and creates FaceVerificationPassed outbox event")
    void testApproveSuccess() {
        FaceVerification fv = FaceVerification.createChallenge(
                "user-1", "kyc-1", "manual", "[\"CHOP_MAT\"]", "nonce", Instant.now(), 1, "v1.0"
        );
        when(faceRepository.findByKycId("kyc-1")).thenReturn(Optional.of(fv));

        FaceDecisionRequest req = new FaceDecisionRequest();
        req.setDecision("PASSED");

        useCase.decide("mod-1", "MODERATOR", "kyc-1", req, 0L, "127.0.0.1");

        assertThat(fv.getStatus()).isEqualTo(FaceVerificationStatus.PASSED);
        verify(outboxRepository).save(argThat(event ->
                "FaceVerificationPassed".equals(event.getEventType())
        ));
    }
}

