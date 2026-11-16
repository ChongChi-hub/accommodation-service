package vn.cozynest.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.cozynest.identity.application.dto.AnonymizeAccountRequest;
import vn.cozynest.identity.application.dto.DataSubjectExportDto;
import vn.cozynest.identity.application.usecase.DataSubjectRightsUseCase;
import vn.cozynest.identity.domain.model.*;
import vn.cozynest.identity.domain.repository.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSubjectRightsUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserVerificationRepository userVerificationRepository;

    @Mock
    private FaceVerificationRepository faceVerificationRepository;

    @Mock
    private FaceVerificationFrameRepository faceVerificationFrameRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private ConsentHistoryRepository consentHistoryRepository;

    @InjectMocks
    private DataSubjectRightsUseCase useCase;

    private User sampleUser;

    @BeforeEach
    void setup() {
        sampleUser = new User(
                "usr-100",
                "test@cozynest.vn",
                "0987654321",
                "hash123",
                "Nguyen Van A",
                "https://avatar.url",
                "MALE",
                null,
                UserStatus.ACTIVE,
                Instant.now(),
                null,
                false,
                Instant.now(),
                null,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("exportPersonalData returns full Decree 13 dossier")
    void testExportPersonalData() {
        when(userRepository.findById("usr-100")).thenReturn(Optional.of(sampleUser));
        when(userProfileRepository.findByUserId("usr-100")).thenReturn(Optional.empty());
        when(userVerificationRepository.findByUserId("usr-100")).thenReturn(Optional.empty());
        when(faceVerificationRepository.findByUserId("usr-100")).thenReturn(Optional.empty());
        when(consentHistoryRepository.findByUserIdOrderByCreatedAtDesc("usr-100")).thenReturn(List.of(
                ConsentHistory.of("usr-100", "TERMS_OF_SERVICE", "v1.2", true, "127.0.0.1", "Chrome", null)
        ));

        DataSubjectExportDto export = useCase.exportPersonalData("usr-100");

        assertThat(export).isNotNull();
        assertThat(export.getComplianceNote()).contains("Nghị định 13/2023/NĐ-CP");
        assertThat(export.getProfile().getFullName()).isEqualTo("Nguyen Van A");
        assertThat(export.getProfile().getPhone()).isEqualTo("098****321");
        assertThat(export.getConsentHistories()).hasSize(1);
    }

    @Test
    @DisplayName("anonymizeAccount scrubs PII, deletes face frames, revokes refresh tokens and sets status DELETED")
    void testAnonymizeAccountSuccess() {
        when(userRepository.findById("usr-100")).thenReturn(Optional.of(sampleUser));

        FaceVerification mockFv = new FaceVerification(
                "fv-100", "usr-100", "kyc-100", "manual",
                FaceVerificationStatus.PASSED, true,
                null, null, null,
                1, Instant.now(), "v1.0",
                null, null, null, null, 1L, Instant.now(), Instant.now()
        );
        when(faceVerificationRepository.findByUserId("usr-100")).thenReturn(Optional.of(mockFv));

        AnonymizeAccountRequest req = AnonymizeAccountRequest.builder()
                .confirm(true)
                .reason("Khong con nhu cau su dung")
                .build();

        useCase.anonymizeAccount("usr-100", req, "127.0.0.1", "Mozilla/5.0");

        assertThat(sampleUser.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(sampleUser.getFullName()).contains("Người dùng đã xóa");
        assertThat(sampleUser.getPhone()).isNull();
        assertThat(sampleUser.getAvatarUrl()).isNull();
        assertThat(sampleUser.getPasswordHash()).isNull();
        assertThat(sampleUser.isAnonymized()).isTrue();

        verify(userRepository, times(1)).save(sampleUser);
        verify(faceVerificationFrameRepository, times(1)).deleteByFaceVerificationId("fv-100");
        verify(refreshTokenRepository, times(1)).revokeAllByUserId("usr-100");
        verify(consentHistoryRepository, times(1)).save(any(ConsentHistory.class));
    }

    @Test
    @DisplayName("anonymizeAccount throws IllegalStateException if account is already anonymized")
    void testAnonymizeAccountAlreadyDeletedThrows() {
        sampleUser.anonymize();
        when(userRepository.findById("usr-100")).thenReturn(Optional.of(sampleUser));

        AnonymizeAccountRequest req = AnonymizeAccountRequest.builder().confirm(true).build();

        assertThrows(IllegalStateException.class, () ->
                useCase.anonymizeAccount("usr-100", req, "127.0.0.1", "Mozilla/5.0"));
    }
}
