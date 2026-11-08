package vn.cozynest.identity.infrastructure.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.identity.application.dto.FaceDecisionRequest;
import vn.cozynest.identity.application.dto.FaceModerationDetailDto;
import vn.cozynest.identity.application.dto.FaceStatusResponse;
import vn.cozynest.identity.application.usecase.FaceModerationUseCase;
import vn.cozynest.identity.application.usecase.LandlordFaceVerificationUseCase;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.FaceVerification;
import vn.cozynest.identity.domain.model.FaceVerificationFrame;
import vn.cozynest.identity.domain.model.FaceVerificationStatus;
import vn.cozynest.identity.domain.port.FacePrivateStoragePort;
import vn.cozynest.identity.domain.repository.*;
import vn.cozynest.identity.interfaces.rest.FaceModerationController;
import vn.cozynest.identity.interfaces.rest.LandlordFaceVerificationController;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FaceVerificationSecurityMatrixTest {

    private LandlordFaceVerificationController landlordController;
    private FaceModerationController moderationController;

    private FaceVerificationRepository faceRepository;
    private FaceVerificationFrameRepository frameRepository;
    private FaceAccessLogRepository accessLogRepository;
    private FacePrivateStoragePort storagePort;
    private FaceCryptoService faceCryptoService;

    @BeforeEach
    void setUp() {
        faceRepository = mock(FaceVerificationRepository.class);
        frameRepository = mock(FaceVerificationFrameRepository.class);
        accessLogRepository = mock(FaceAccessLogRepository.class);
        storagePort = mock(FacePrivateStoragePort.class);
        faceCryptoService = mock(FaceCryptoService.class);
        FaceEventOutboxRepository outboxRepository = mock(FaceEventOutboxRepository.class);
        UserVerificationRepository userVerificationRepo = mock(UserVerificationRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        KycCryptoService kycCryptoService = mock(KycCryptoService.class);

        FaceModerationUseCase moderationUseCase = new FaceModerationUseCase(
                faceRepository, frameRepository, accessLogRepository, outboxRepository,
                userVerificationRepo, userRepo, storagePort, faceCryptoService, kycCryptoService
        );
        moderationController = new FaceModerationController(moderationUseCase);

        LandlordFaceVerificationUseCase landlordUseCase = mock(LandlordFaceVerificationUseCase.class);
        when(landlordUseCase.getFaceStatus("u-landlord-01")).thenReturn(
                FaceStatusResponse.builder().status("PASSED").provider("manual").build()
        );
        landlordController = new LandlordFaceVerificationController(landlordUseCase);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Role: LANDLORD can query own face verification status")
    void testLandlordCanGetStatus() {
        var auth = new UsernamePasswordAuthenticationToken("u-landlord-01", null, List.of(new SimpleGrantedAuthority("ROLE_LANDLORD")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        ResponseEntity<ApiResponse<FaceStatusResponse>> res = landlordController.getStatus(auth);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getData().getStatus()).isEqualTo("PASSED");
    }

    @Test
    @DisplayName("Self-Review Prevention: Reviewer cannot review own face verification")
    void testSelfReviewForbidden() {
        FaceVerification fv = FaceVerification.createChallenge(
                "u-mod-01", "uv-1", "manual", "[\"CHOP_MAT\"]", "nonce", Instant.now(), 1, "v1.0"
        );
        when(faceRepository.findByKycId("uv-1")).thenReturn(Optional.of(fv));

        var auth = new UsernamePasswordAuthenticationToken("u-mod-01", null, List.of(new SimpleGrantedAuthority("ROLE_MODERATOR")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> moderationController.getDetail("uv-1", auth, request))
                .isInstanceOf(IdentityDomainException.class)
                .hasMessageContaining("không được phép tự duyệt");
    }

    @Test
    @DisplayName("Stream Frame: Stream returns no-store and logs exactly 1 access log")
    void testStreamFrameAuditedAndNoStore() {
        FaceVerification fv = FaceVerification.createChallenge(
                "u-landlord-01", "uv-1", "manual", "[\"CHOP_MAT\"]", "nonce", Instant.now(), 1, "v1.0"
        );
        FaceVerificationFrame frame = FaceVerificationFrame.create(fv.getId(), "NEUTRAL", "key1", "hash", 100);

        when(faceRepository.findByKycId("uv-1")).thenReturn(Optional.of(fv));
        when(frameRepository.findById("frame-1")).thenReturn(Optional.of(frame));
        when(storagePort.load("key1")).thenReturn(new byte[]{1, 2, 3});
        when(faceCryptoService.decrypt(any())).thenReturn(new byte[]{4, 5, 6});

        var auth = new UsernamePasswordAuthenticationToken("u-mod-01", null, List.of(new SimpleGrantedAuthority("ROLE_MODERATOR")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest();
        ResponseEntity<byte[]> res = moderationController.streamFrame("uv-1", "frame-1", auth, request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getHeaders().getCacheControl()).contains("no-store");
        assertThat(res.getBody()).containsExactly(4, 5, 6);
        verify(accessLogRepository, times(1)).save(any());
    }
}

