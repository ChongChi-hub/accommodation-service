package vn.cozynest.identity.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import vn.cozynest.identity.application.dto.LandlordKycStatusResponse;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.KycStatus;
import vn.cozynest.identity.domain.model.UserVerification;
import vn.cozynest.identity.domain.port.KycPrivateStoragePort;
import vn.cozynest.identity.domain.repository.KycEventOutboxRepository;
import vn.cozynest.identity.domain.repository.KycStatusHistoryRepository;
import vn.cozynest.identity.domain.repository.UserVerificationRepository;
import vn.cozynest.identity.infrastructure.security.KycCryptoService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LandlordKycUseCaseTest {

    @Mock
    private UserVerificationRepository verificationRepository;

    @Mock
    private KycStatusHistoryRepository historyRepository;

    @Mock
    private KycEventOutboxRepository outboxRepository;

    @Mock
    private KycPrivateStoragePort kycStorage;

    private KycCryptoService cryptoService;
    private LandlordKycUseCase useCase;

    @BeforeEach
    void setUp() {
        cryptoService = new KycCryptoService(
                "CozyNestKycSecretKey2026DevEnvironment32B",
                "CozyNestKycPepperSecret2026DevEnvKey!"
        );
        useCase = new LandlordKycUseCase(
                verificationRepository,
                historyRepository,
                outboxRepository,
                kycStorage,
                cryptoService
        );
    }

    private MockMultipartFile createMockImage(String name) throws IOException {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(100, 100, java.awt.image.BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", baos);
        return new MockMultipartFile(name, name + ".png", "image/png", baos.toByteArray());
    }

    @Test
    @DisplayName("Should return NONE status when landlord has never submitted KYC")
    void shouldReturnNoneStatusWhenEmpty() {
        when(verificationRepository.findByUserId("u-landlord-01")).thenReturn(Optional.empty());

        LandlordKycStatusResponse status = useCase.getKycStatus("u-landlord-01");

        assertThat(status.getStatus()).isEqualTo(KycStatus.NONE.name());
        assertThat(status.isCanResubmit()).isTrue();
        assertThat(status.getRemainingAttempts()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should submit KYC successfully when data and images are valid")
    void shouldSubmitKycSuccessfully() throws IOException {
        MockMultipartFile front = createMockImage("front");
        MockMultipartFile back = createMockImage("back");

        when(verificationRepository.findByUserId("u-landlord-01")).thenReturn(Optional.empty());
        when(verificationRepository.existsByActiveIdHashAndUserIdNot(anyString(), eq("u-landlord-01"))).thenReturn(false);
        when(verificationRepository.save(any(UserVerification.class))).thenAnswer(i -> i.getArgument(0));

        LandlordKycStatusResponse res = useCase.submitKyc(
                "u-landlord-01", front, back, "Nguyen Van Toan", "000085000001",
                LocalDate.of(1985, 5, 15), LocalDate.of(2021, 6, 10), "CA TP HCM", true
        );

        assertThat(res.getStatus()).isEqualTo(KycStatus.PENDING.name());
        assertThat(res.getIdNumberMasked()).isEqualTo("********0001");
        verify(kycStorage, times(2)).store(anyString(), any(), eq("image/webp"));
        verify(outboxRepository, times(1)).save(any());
        verify(historyRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should reject submission when citizen ID number already belongs to another account")
    void shouldRejectDuplicateCitizenId() throws IOException {
        MockMultipartFile front = createMockImage("front");
        MockMultipartFile back = createMockImage("back");

        when(verificationRepository.findByUserId("u-landlord-02")).thenReturn(Optional.empty());
        when(verificationRepository.existsByActiveIdHashAndUserIdNot(anyString(), eq("u-landlord-02"))).thenReturn(true);

        IdentityDomainException ex = assertThrows(IdentityDomainException.class, () ->
                useCase.submitKyc(
                        "u-landlord-02", front, back, "Tran Minh Tuan", "000085000001",
                        LocalDate.of(1988, 9, 20), LocalDate.of(2021, 6, 10), "CA TP HCM", true
                )
        );

        assertThat(ex.getErrorCode()).isEqualTo("ERR_KYC_ID_NUMBER_DUPLICATE");
    }

    @Test
    @DisplayName("Should reject submission when consent is false")
    void shouldRejectWithoutConsent() throws IOException {
        MockMultipartFile front = createMockImage("front");
        MockMultipartFile back = createMockImage("back");

        IdentityDomainException ex = assertThrows(IdentityDomainException.class, () ->
                useCase.submitKyc(
                        "u-landlord-01", front, back, "Nguyen Van Toan", "000085000001",
                        LocalDate.of(1985, 5, 15), LocalDate.of(2021, 6, 10), "CA TP HCM", false
                )
        );

        assertThat(ex.getErrorCode()).isEqualTo("ERR_KYC_CONSENT_REQUIRED");
    }
}

