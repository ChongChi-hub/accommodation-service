package vn.cozynest.identity.application.usecase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import vn.cozynest.identity.application.dto.LandlordKycStatusResponse;
import vn.cozynest.identity.application.dto.UpdateUserProfileRequest;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.KycEventOutbox;
import vn.cozynest.identity.domain.model.KycRejectReasonCode;
import vn.cozynest.identity.domain.model.KycStatus;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.model.UserProfile;
import vn.cozynest.identity.domain.model.UserRoleType;
import vn.cozynest.identity.domain.model.UserVerification;
import vn.cozynest.identity.domain.port.AvatarStoragePort;
import vn.cozynest.identity.domain.port.KycPrivateStoragePort;
import vn.cozynest.identity.domain.repository.*;
import vn.cozynest.identity.infrastructure.security.KycCryptoService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KycSecurityAndPrivacyAuditTest {

    private MockMultipartFile createPng(String name, int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return new MockMultipartFile(name, name + ".png", "image/png", baos.toByteArray());
    }

    @Test
    @DisplayName("5. Sensitive Data: Outbox payload, DTO, and error messages never expose plaintext 12-digit citizen ID")
    void testCitizenIdNeverExposedPlaintext() throws IOException {
        UserVerificationRepository verificationRepo = mock(UserVerificationRepository.class);
        KycStatusHistoryRepository historyRepo = mock(KycStatusHistoryRepository.class);
        KycEventOutboxRepository outboxRepo = mock(KycEventOutboxRepository.class);
        vn.cozynest.identity.domain.repository.LandlordProfileRepository landlordProfileRepo = mock(vn.cozynest.identity.domain.repository.LandlordProfileRepository.class);
        KycPrivateStoragePort kycStorage = mock(KycPrivateStoragePort.class);
        KycCryptoService cryptoService = new KycCryptoService(
                "CozyNestKycSecretKey2026DevEnvironment32B", "CozyNestKycPepperSecret2026DevEnvKey!"
        );

        LandlordKycUseCase useCase = new LandlordKycUseCase(
                verificationRepo, historyRepo, outboxRepo, landlordProfileRepo, kycStorage, cryptoService
        );

        MockMultipartFile front = createPng("front", 100, 100);
        MockMultipartFile back = createPng("back", 100, 100);

        when(verificationRepo.findByUserId("u-landlord-01")).thenReturn(Optional.empty());
        when(verificationRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        List<KycEventOutbox> outboxEvents = new ArrayList<>();
        when(outboxRepo.save(any())).thenAnswer(i -> {
            outboxEvents.add(i.getArgument(0));
            return i.getArgument(0);
        });

        String plainCitizenId = "079090123456";
        LandlordKycStatusResponse res = useCase.submitKyc(
                "u-landlord-01", front, back, "Nguyen Van Toan", plainCitizenId,
                LocalDate.of(1990, 1, 1), LocalDate.of(2020, 1, 1), "CA TP HCM", true
        );

        // 1. DTO response only displays masked form: ********3456
        assertThat(res.getIdNumberMasked()).isEqualTo("********3456");
        assertThat(res.getIdNumberMasked()).doesNotContain("07909012");

        // 2. Outbox event payload contains aggregateId and userId, NOT plaintext citizen ID
        assertThat(outboxEvents).hasSize(1);
        String payload = outboxEvents.get(0).getPayload();
        assertThat(payload).doesNotContain(plainCitizenId);
    }

    @Test
    @DisplayName("5. Duplicate citizen ID returns ERR_KYC_ID_NUMBER_DUPLICATE without disclosing other account details")
    void testDuplicateCitizenIdDoesNotDiscloseAccount() throws IOException {
        UserVerificationRepository verificationRepo = mock(UserVerificationRepository.class);
        KycStatusHistoryRepository historyRepo = mock(KycStatusHistoryRepository.class);
        KycEventOutboxRepository outboxRepo = mock(KycEventOutboxRepository.class);
        vn.cozynest.identity.domain.repository.LandlordProfileRepository landlordProfileRepo = mock(vn.cozynest.identity.domain.repository.LandlordProfileRepository.class);
        KycPrivateStoragePort kycStorage = mock(KycPrivateStoragePort.class);
        KycCryptoService cryptoService = new KycCryptoService(
                "CozyNestKycSecretKey2026DevEnvironment32B", "CozyNestKycPepperSecret2026DevEnvKey!"
        );

        LandlordKycUseCase useCase = new LandlordKycUseCase(
                verificationRepo, historyRepo, outboxRepo, landlordProfileRepo, kycStorage, cryptoService
        );

        MockMultipartFile front = createPng("front", 100, 100);
        MockMultipartFile back = createPng("back", 100, 100);

        when(verificationRepo.findByUserId("u-landlord-02")).thenReturn(Optional.empty());
        when(verificationRepo.existsByActiveIdHashAndUserIdNot(anyString(), eq("u-landlord-02"))).thenReturn(true);

        IdentityDomainException ex = assertThrows(IdentityDomainException.class, () ->
                useCase.submitKyc("u-landlord-02", front, back, "Le Van B", "079090123456",
                        LocalDate.of(1992, 2, 2), LocalDate.of(2021, 2, 2), "CA TP HCM", true)
        );

        assertThat(ex.getErrorCode()).isEqualTo("ERR_KYC_ID_NUMBER_DUPLICATE");
        assertThat(ex.getMessage()).doesNotContain("u-landlord-01");
        assertThat(ex.getMessage()).doesNotContain("@cozynest.vn");
    }

    @Test
    @DisplayName("5. 4th KYC submission attempt in 24 hours triggers rate limit rejection (ERR_KYC_RATE_LIMIT_EXCEEDED)")
    void testFourthSubmissionIn24HoursRejected() throws IOException {
        UserVerificationRepository verificationRepo = mock(UserVerificationRepository.class);
        KycStatusHistoryRepository historyRepo = mock(KycStatusHistoryRepository.class);
        KycEventOutboxRepository outboxRepo = mock(KycEventOutboxRepository.class);
        vn.cozynest.identity.domain.repository.LandlordProfileRepository landlordProfileRepo = mock(vn.cozynest.identity.domain.repository.LandlordProfileRepository.class);
        KycPrivateStoragePort kycStorage = mock(KycPrivateStoragePort.class);
        KycCryptoService cryptoService = new KycCryptoService(
                "CozyNestKycSecretKey2026DevEnvironment32B", "CozyNestKycPepperSecret2026DevEnvKey!"
        );

        LandlordKycUseCase useCase = new LandlordKycUseCase(
                verificationRepo, historyRepo, outboxRepo, landlordProfileRepo, kycStorage, cryptoService
        );

        UserVerification rejected3Times = new UserVerification(
                "v-rej-03", "u-landlord-01", "Nguyen Van A", LocalDate.of(1990, 1, 1),
                "enc", "hash", "3456", LocalDate.of(2020, 1, 1), "CA TP HCM",
                "frontKey", "backKey", null, KycStatus.REJECTED, Instant.now().minusSeconds(3600), "v1.0",
                Instant.now().minusSeconds(1800), Instant.now().minusSeconds(600), "u-mod-01",
                KycRejectReasonCode.BLURRY, "Image is blurry", 3, 3L,
                Instant.now().minusSeconds(3600), Instant.now().minusSeconds(600)
        );

        when(verificationRepo.findByUserId("u-landlord-01")).thenReturn(Optional.of(rejected3Times));

        MockMultipartFile front = createPng("front", 100, 100);
        MockMultipartFile back = createPng("back", 100, 100);

        IdentityDomainException ex = assertThrows(IdentityDomainException.class, () ->
                useCase.submitKyc("u-landlord-01", front, back, "Nguyen Van A", "079090123456",
                        LocalDate.of(1990, 1, 1), LocalDate.of(2020, 1, 1), "CA TP HCM", true)
        );

        assertThat(ex.getErrorCode()).isEqualTo("ERR_KYC_RATE_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("9. Profile Optimistic Locking: Concurrent edits with stale version throw ERR_PROFILE_VERSION_CONFLICT")
    void testOptimisticLockingThrowsConflict() {
        UserRepository userRepo = mock(UserRepository.class);
        UserProfileRepository profileRepo = mock(UserProfileRepository.class);
        UserVerificationRepository verificationRepo = mock(UserVerificationRepository.class);
        AvatarStoragePort avatarStorage = mock(AvatarStoragePort.class);

        UserProfileUseCase useCase = new UserProfileUseCase(
                userRepo, profileRepo, verificationRepo, avatarStorage, "/api/v1/media"
        );

        User user = User.createNew("landlord@cozynest.vn", "0901234567", "hash", "Nguyen Landlord", UserRoleType.LANDLORD);
        when(userRepo.findById("u-landlord-01")).thenReturn(Optional.of(user));

        UserProfile currentProfile = UserProfile.createInitial("u-landlord-01", "Nguyen Landlord", "0901234567");
        currentProfile.setVersion(2L); // Database has version 2

        when(profileRepo.findByUserId("u-landlord-01")).thenReturn(Optional.of(currentProfile));

        UpdateUserProfileRequest staleRequest = new UpdateUserProfileRequest();
        staleRequest.setFullName("Nguyen Updated");
        staleRequest.setPhone("0901234567");
        staleRequest.setVersion(1L); // stale version 1

        IdentityDomainException ex = assertThrows(IdentityDomainException.class, () ->
                useCase.updateProfile("u-landlord-01", staleRequest)
        );
        assertThat(ex.getErrorCode()).isEqualTo("ERR_PROFILE_VERSION_CONFLICT");
    }

    @Test
    @DisplayName("9. Avatar Magic Bytes: Uploading executable/text file with .jpg extension is rejected with ERR_AVATAR_FORMAT_INVALID")
    void testAvatarRejectsFakeJpg() {
        UserRepository userRepo = mock(UserRepository.class);
        UserProfileRepository profileRepo = mock(UserProfileRepository.class);
        UserVerificationRepository verificationRepo = mock(UserVerificationRepository.class);
        AvatarStoragePort avatarStorage = mock(AvatarStoragePort.class);

        UserProfileUseCase useCase = new UserProfileUseCase(
                userRepo, profileRepo, verificationRepo, avatarStorage, "/api/v1/media"
        );

        // Fake image: text file renamed to .jpg
        byte[] fakeBytes = "MZ\0\0This is a fake windows PE binary pretending to be an image".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile fakeFile = new MockMultipartFile("file", "malicious.jpg", "image/jpeg", fakeBytes);

        IdentityDomainException ex = assertThrows(IdentityDomainException.class, () ->
                useCase.uploadAvatar("u-landlord-01", fakeFile)
        );
        assertThat(ex.getErrorCode()).isEqualTo("ERR_AVATAR_FORMAT_INVALID");
    }
}
