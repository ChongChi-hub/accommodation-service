package vn.cozynest.identity.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import vn.cozynest.identity.application.dto.FaceChallengeResponse;
import vn.cozynest.identity.application.dto.FaceStatusResponse;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.*;
import vn.cozynest.identity.domain.port.FacePrivateStoragePort;
import vn.cozynest.identity.domain.port.FaceVerificationProvider;
import vn.cozynest.identity.domain.repository.FaceVerificationFrameRepository;
import vn.cozynest.identity.domain.repository.FaceVerificationRepository;
import vn.cozynest.identity.domain.repository.UserVerificationRepository;
import vn.cozynest.identity.infrastructure.ekyc.ManualReviewProvider;
import vn.cozynest.identity.infrastructure.security.FaceCryptoService;
import vn.cozynest.identity.infrastructure.security.FaceImageValidator;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LandlordFaceVerificationUseCaseTest {

    @Mock
    private FaceVerificationRepository faceRepository;
    @Mock
    private FaceVerificationFrameRepository frameRepository;
    @Mock
    private UserVerificationRepository userVerificationRepository;
    @Mock
    private FacePrivateStoragePort storagePort;
    @Mock
    private FaceCryptoService cryptoService;
    @Mock
    private FaceImageValidator imageValidator;

    private LandlordFaceVerificationUseCase useCase;

    @BeforeEach
    void setUp() {
        FaceVerificationProvider manualProvider = new ManualReviewProvider();
        useCase = new LandlordFaceVerificationUseCase(
                faceRepository, frameRepository, userVerificationRepository,
                storagePort, cryptoService, imageValidator, List.of(manualProvider), "manual"
        );
    }

    @Test
    @DisplayName("Challenge: Fails if consent is not given")
    void testCreateChallengeConsentRequired() {
        assertThatThrownBy(() -> useCase.createChallenge("user-1", false))
                .isInstanceOf(IdentityDomainException.class)
                .hasMessageContaining("Bạn phải đồng ý riêng");
    }

    @Test
    @DisplayName("Challenge: Fails if document KYC is missing")
    void testCreateChallengeDocRequired() {
        when(userVerificationRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.createChallenge("user-1", true))
                .isInstanceOf(IdentityDomainException.class)
                .hasMessageContaining("Bạn cần hoàn tất nộp giấy tờ");
    }

    @Test
    @DisplayName("Challenge: Fails when exceeding 3 attempts in 24 hours")
    void testCreateChallengeRateLimitExceeded() {
        UserVerification kyc = mock(UserVerification.class);
        when(kyc.getStatus()).thenReturn(KycStatus.PENDING);
        when(userVerificationRepository.findByUserId("user-1")).thenReturn(Optional.of(kyc));
        when(faceRepository.countAttemptsInLast24Hours(eq("user-1"), any(Instant.class))).thenReturn(3);

        assertThatThrownBy(() -> useCase.createChallenge("user-1", true))
                .isInstanceOf(IdentityDomainException.class)
                .hasMessageContaining("vượt quá số lần thử");
    }

    @Test
    @DisplayName("Challenge: Generates 2 random actions and single-use nonce")
    void testCreateChallengeSuccess() {
        UserVerification kyc = mock(UserVerification.class);
        when(kyc.getId()).thenReturn("kyc-123");
        when(kyc.getStatus()).thenReturn(KycStatus.PENDING);
        when(userVerificationRepository.findByUserId("user-1")).thenReturn(Optional.of(kyc));
        when(faceRepository.countAttemptsInLast24Hours(eq("user-1"), any(Instant.class))).thenReturn(0);
        when(faceRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        FaceChallengeResponse res = useCase.createChallenge("user-1", true);

        assertThat(res.getNonce()).isNotBlank();
        assertThat(res.getActions()).hasSize(2);
        assertThat(res.getAttemptNo()).isEqualTo(1);
        assertThat(res.getRemainingAttempts()).isEqualTo(2);
        verify(faceRepository).save(any(FaceVerification.class));
    }

    @Test
    @DisplayName("Submission: Fails when nonce does not match")
    void testSubmitFramesNonceMismatch() {
        FaceVerification fv = FaceVerification.createChallenge(
                "user-1", "kyc-1", "manual", "[\"CHOP_MAT\",\"QUAY_TRAI\"]",
                "valid_nonce", Instant.now().plusSeconds(120), 1, "v1.0"
        );
        when(faceRepository.findByUserId("user-1")).thenReturn(Optional.of(fv));

        MockMultipartFile file = new MockMultipartFile("f", "a.jpg", "image/jpeg", new byte[]{1});

        assertThatThrownBy(() -> useCase.submitFrames("user-1", "wrong_nonce", false, file, List.of(file, file)))
                .isInstanceOf(IdentityDomainException.class)
                .hasMessageContaining("không hợp lệ hoặc đã qua sử dụng");
    }

    @Test
    @DisplayName("Submission: Fails when challenge expired")
    void testSubmitFramesExpired() {
        FaceVerification fv = FaceVerification.createChallenge(
                "user-1", "kyc-1", "manual", "[\"CHOP_MAT\",\"QUAY_TRAI\"]",
                "valid_nonce", Instant.now().minusSeconds(10), 1, "v1.0"
        );
        when(faceRepository.findByUserId("user-1")).thenReturn(Optional.of(fv));

        MockMultipartFile file = new MockMultipartFile("f", "a.jpg", "image/jpeg", new byte[]{1});

        assertThatThrownBy(() -> useCase.submitFrames("user-1", "valid_nonce", false, file, List.of(file, file)))
                .isInstanceOf(IdentityDomainException.class)
                .hasMessageContaining("đã hết hạn");
    }

    @Test
    @DisplayName("Revoke Consent: Purges all encrypted frames and marks deleted")
    void testRevokeConsentPurgesFrames() {
        FaceVerification fv = FaceVerification.createChallenge(
                "user-1", "kyc-1", "manual", "[\"CHOP_MAT\",\"QUAY_TRAI\"]",
                "nonce", Instant.now().plusSeconds(120), 1, "v1.0"
        );
        when(faceRepository.findByUserId("user-1")).thenReturn(Optional.of(fv));

        FaceVerificationFrame frame = FaceVerificationFrame.create(fv.getId(), "NEUTRAL", "key1", "hash", 100);
        when(frameRepository.findActiveByFaceVerificationId(fv.getId())).thenReturn(List.of(frame));

        useCase.revokeConsentAndPurge("user-1");

        verify(storagePort).delete("key1");
        verify(frameRepository).save(argThat(FaceVerificationFrame::isDeleted));
    }
}

