package vn.cozynest.identity.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vn.cozynest.identity.application.dto.RejectKycRequest;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.*;
import vn.cozynest.identity.domain.port.KycPrivateStoragePort;
import vn.cozynest.identity.domain.repository.*;
import vn.cozynest.identity.infrastructure.security.KycCryptoService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KycApprovalAndRoleGrantUnitTest {

    private UserVerificationRepository verificationRepository;
    private UserRepository userRepository;
    private LandlordProfileRepository landlordProfileRepository;
    private KycStatusHistoryRepository historyRepository;
    private KycAccessLogRepository accessLogRepository;
    private KycEventOutboxRepository outboxRepository;
    private KycPrivateStoragePort kycStorage;
    private KycCryptoService cryptoService;
    private KycModerationUseCase moderationUseCase;

    @BeforeEach
    void setUp() {
        verificationRepository = mock(UserVerificationRepository.class);
        userRepository = mock(UserRepository.class);
        landlordProfileRepository = mock(LandlordProfileRepository.class);
        historyRepository = mock(KycStatusHistoryRepository.class);
        accessLogRepository = mock(KycAccessLogRepository.class);
        outboxRepository = mock(KycEventOutboxRepository.class);
        kycStorage = mock(KycPrivateStoragePort.class);
        cryptoService = mock(KycCryptoService.class);

        moderationUseCase = new KycModerationUseCase(
                verificationRepository, userRepository, landlordProfileRepository,
                historyRepository, accessLogRepository, outboxRepository, kycStorage, cryptoService
        );
    }

    @Test
    @DisplayName("R2: Approving KYC grants LANDLORD role in user and sets LandlordProfile to APPROVED")
    void testApproveKycGrantsLandlordRole() {
        String userId = "applicant-user-01";
        String kycId = "kyc-001";
        String reviewerId = "admin-moderator-01";

        User user = new User(
                userId, "applicant@example.com", "0901234567", "pwd", "Applicant",
                null, null, null, UserStatus.ACTIVE, Instant.now(), null,
                new HashSet<>(Set.of(UserRoleType.TENANT)), Instant.now(), Instant.now()
        );

        UserVerification verification = new UserVerification(
                kycId, userId, "Applicant Name", LocalDate.of(1995, 5, 5),
                "enc", "hash", "1234", LocalDate.of(2020, 1, 1), "CA Hanoi",
                "frontKey", "backKey", null, KycStatus.PENDING, Instant.now(), "v1.0",
                null, null, null, null, null, 1, 0L, Instant.now(), Instant.now()
        );

        LandlordProfile profile = LandlordProfile.createNew(userId);
        profile.submitKyc(); // PENDING_REVIEW

        when(verificationRepository.findById(kycId)).thenReturn(Optional.of(verification));
        when(verificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(landlordProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(landlordProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        moderationUseCase.approveKyc(reviewerId, "ADMIN", kycId, 0L, "127.0.0.1");

        // Assert user has LANDLORD role granted
        assertThat(user.getRoles()).contains(UserRoleType.TENANT, UserRoleType.LANDLORD);
        verify(userRepository).save(user);

        // Assert LandlordProfile is APPROVED
        assertThat(profile.getApprovalStatus()).isEqualTo(LandlordApprovalStatus.APPROVED);
        assertThat(profile.getReviewedBy()).isEqualTo(reviewerId);
        verify(landlordProfileRepository).save(profile);

        // Assert outbox event LandlordApproved
        ArgumentCaptor<KycEventOutbox> outboxCaptor = ArgumentCaptor.forClass(KycEventOutbox.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("LandlordApproved");
    }

    @Test
    @DisplayName("R8: Reviewer cannot approve their own application (ERR_KYC_SELF_REVIEW_FORBIDDEN)")
    void testSelfReviewApproveForbidden() {
        String userId = "admin-user-01";
        String kycId = "kyc-002";
        String reviewerId = "admin-user-01"; // Same!

        UserVerification verification = new UserVerification(
                kycId, userId, "Admin Self", LocalDate.of(1990, 1, 1),
                "enc", "hash", "1234", LocalDate.of(2020, 1, 1), "CA Hanoi",
                "frontKey", "backKey", null, KycStatus.PENDING, Instant.now(), "v1.0",
                null, null, null, null, null, 1, 0L, Instant.now(), Instant.now()
        );

        when(verificationRepository.findById(kycId)).thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> moderationUseCase.approveKyc(reviewerId, "ADMIN", kycId, 0L, "127.0.0.1"))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_KYC_SELF_REVIEW_FORBIDDEN");

        verify(userRepository, never()).save(any());
        verify(landlordProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("R8: Reviewer cannot reject their own application (ERR_KYC_SELF_REVIEW_FORBIDDEN)")
    void testSelfReviewRejectForbidden() {
        String userId = "admin-user-01";
        String kycId = "kyc-003";
        String reviewerId = "admin-user-01"; // Same!

        UserVerification verification = new UserVerification(
                kycId, userId, "Admin Self", LocalDate.of(1990, 1, 1),
                "enc", "hash", "1234", LocalDate.of(2020, 1, 1), "CA Hanoi",
                "frontKey", "backKey", null, KycStatus.PENDING, Instant.now(), "v1.0",
                null, null, null, null, null, 1, 0L, Instant.now(), Instant.now()
        );

        when(verificationRepository.findById(kycId)).thenReturn(Optional.of(verification));

        RejectKycRequest req = new RejectKycRequest();
        req.setReasonCode(KycRejectReasonCode.BLURRY);
        req.setReasonNote("Self reject test");

        assertThatThrownBy(() -> moderationUseCase.rejectKyc(reviewerId, "ADMIN", kycId, req, 0L, "127.0.0.1"))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_KYC_SELF_REVIEW_FORBIDDEN");
    }

    @Test
    @DisplayName("R3: Rejecting KYC sets LandlordProfile to REJECTED and emits LandlordRejected")
    void testRejectKycSetsLandlordProfileRejected() {
        String userId = "applicant-user-02";
        String kycId = "kyc-004";
        String reviewerId = "moderator-01";

        UserVerification verification = new UserVerification(
                kycId, userId, "Applicant Name", LocalDate.of(1995, 5, 5),
                "enc", "hash", "1234", LocalDate.of(2020, 1, 1), "CA Hanoi",
                "frontKey", "backKey", null, KycStatus.PENDING, Instant.now(), "v1.0",
                null, null, null, null, null, 1, 0L, Instant.now(), Instant.now()
        );

        LandlordProfile profile = LandlordProfile.createNew(userId);
        profile.submitKyc(); // PENDING_REVIEW

        when(verificationRepository.findById(kycId)).thenReturn(Optional.of(verification));
        when(verificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(landlordProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(landlordProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RejectKycRequest req = new RejectKycRequest();
        req.setReasonCode(KycRejectReasonCode.BLURRY);
        req.setReasonNote("Ảnh căn cước công dân bị mờ");

        moderationUseCase.rejectKyc(reviewerId, "MODERATOR", kycId, req, 0L, "127.0.0.1");

        assertThat(profile.getApprovalStatus()).isEqualTo(LandlordApprovalStatus.REJECTED);
        assertThat(profile.getRejectReasonCode()).isEqualTo("BLURRY");
        assertThat(profile.getRejectReasonNote()).isEqualTo("Ảnh căn cước công dân bị mờ");

        ArgumentCaptor<KycEventOutbox> outboxCaptor = ArgumentCaptor.forClass(KycEventOutbox.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("LandlordRejected");
    }
}
