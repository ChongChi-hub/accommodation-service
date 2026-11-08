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
import vn.cozynest.identity.application.dto.InternalKycStatusResponse;
import vn.cozynest.identity.application.dto.KycModerationDetailDto;
import vn.cozynest.identity.application.dto.LandlordKycStatusResponse;
import vn.cozynest.identity.application.service.InternalUserService;
import vn.cozynest.identity.application.usecase.KycModerationUseCase;
import vn.cozynest.identity.application.usecase.LandlordKycUseCase;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.KycAccessAction;
import vn.cozynest.identity.domain.model.KycAccessLog;
import vn.cozynest.identity.domain.model.KycStatus;
import vn.cozynest.identity.domain.port.KycPrivateStoragePort;
import vn.cozynest.identity.domain.repository.KycAccessLogRepository;
import vn.cozynest.identity.domain.repository.KycStatusHistoryRepository;
import vn.cozynest.identity.domain.repository.UserVerificationRepository;
import vn.cozynest.identity.interfaces.rest.InternalUserController;
import vn.cozynest.identity.interfaces.rest.KycModerationController;
import vn.cozynest.identity.interfaces.rest.LandlordKycController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KycSecurityMatrixTest {

    private InternalUserController internalUserController;
    private UserVerificationRepository verificationRepository;
    private KycAccessLogRepository accessLogRepository;
    private KycModerationUseCase moderationUseCase;
    private KycModerationController moderationController;
    private LandlordKycUseCase landlordKycUseCase;
    private LandlordKycController landlordKycController;

    private final List<KycAccessLog> recordedAccessLogs = new ArrayList<>();

    @BeforeEach
    void setUp() {
        verificationRepository = mock(UserVerificationRepository.class);
        vn.cozynest.identity.domain.repository.FaceVerificationRepository faceVerificationRepository = mock(vn.cozynest.identity.domain.repository.FaceVerificationRepository.class);
        when(faceVerificationRepository.findByUserId(anyString())).thenReturn(java.util.Optional.of(
                new vn.cozynest.identity.domain.model.FaceVerification(
                        "fv-1", "u-1", "uv-1", "manual", vn.cozynest.identity.domain.model.FaceVerificationStatus.PASSED,
                        true, null, null, null, 1, null, null, null, null, null, null, 0L, null, null
                )
        ));
        accessLogRepository = mock(KycAccessLogRepository.class);
        when(accessLogRepository.save(any(KycAccessLog.class))).thenAnswer(i -> {
            KycAccessLog log = i.getArgument(0);
            recordedAccessLogs.add(log);
            return log;
        });

        InternalUserService internalService = new InternalUserService(verificationRepository, faceVerificationRepository, true);
        internalUserController = new InternalUserController(internalService, "CozyNestInternalServiceKeySecret2026", null);

        KycPrivateStoragePort kycStorage = mock(KycPrivateStoragePort.class);
        when(kycStorage.load("kyc/private/u-landlord-01_front.webp")).thenReturn(new byte[]{1, 2, 3});

        KycCryptoService cryptoService = new KycCryptoService("CozyNestKycSecretKey2026DevEnvironment32B", "CozyNestKycPepperSecret2026DevEnvKey!", null);
        KycStatusHistoryRepository historyRepo = mock(KycStatusHistoryRepository.class);
        vn.cozynest.identity.domain.repository.UserRepository userRepo = mock(vn.cozynest.identity.domain.repository.UserRepository.class);
        vn.cozynest.identity.domain.repository.KycEventOutboxRepository outboxRepo = mock(vn.cozynest.identity.domain.repository.KycEventOutboxRepository.class);

        moderationUseCase = new KycModerationUseCase(
                verificationRepository, userRepo, historyRepo, accessLogRepository, outboxRepo, kycStorage, cryptoService
        );
        moderationController = new KycModerationController(moderationUseCase);

        landlordKycUseCase = new LandlordKycUseCase(
                verificationRepository, historyRepo, outboxRepo, kycStorage, cryptoService
        );
        landlordKycController = new LandlordKycController(landlordKycUseCase);

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        recordedAccessLogs.clear();
    }

    private void authenticate(String userId, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("3. Internal KYC Status: Rejects missing/invalid service key with 401 UNAUTHORIZED")
    void testInternalKeyRejection() {
        ResponseEntity<ApiResponse<InternalKycStatusResponse>> resMissing =
                internalUserController.getKycStatus("u-landlord-01", null);
        assertThat(resMissing.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<ApiResponse<InternalKycStatusResponse>> resWrong =
                internalUserController.getKycStatus("u-landlord-01", "tampered-key");
        assertThat(resWrong.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("3. IDOR Protection: Landlord A attempting to access Landlord B KYC image returns 404 Not Found")
    void testIdorLandlordCannotAccessOtherLandlordKyc() {
        authenticate("u-landlord-A", "LANDLORD");
        when(verificationRepository.findByUserId("u-landlord-A")).thenReturn(Optional.empty());

        IdentityDomainException ex = assertThrows(IdentityDomainException.class, () ->
                landlordKycController.streamKycImage(
                        SecurityContextHolder.getContext().getAuthentication(),
                        "front"
                )
        );
        assertThat(ex.getErrorCode()).isEqualTo("ERR_KYC_NOT_FOUND");
    }

    @Test
    @DisplayName("3. Moderator audit trail: Opening KYC image records exactly 1 access log in database")
    void testModeratorViewingImageCreatesAuditLog() {
        authenticate("u-mod-01", "MODERATOR");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");

        vn.cozynest.identity.domain.model.UserVerification v = new vn.cozynest.identity.domain.model.UserVerification(
                "v-01", "u-landlord-01", "Nguyen Van A", java.time.LocalDate.of(1990, 1, 1),
                "enc", "hash", "1234", java.time.LocalDate.of(2020, 1, 1), "CA TP HCM",
                "kyc/private/u-landlord-01_front.webp", "kyc/private/u-landlord-01_back.webp", null,
                KycStatus.PENDING, java.time.Instant.now(), "v1.0",
                java.time.Instant.now(), null, null, null, null, 1, 0L,
                java.time.Instant.now(), java.time.Instant.now()
        );
        when(verificationRepository.findById("v-01")).thenReturn(Optional.of(v));

        ResponseEntity<byte[]> response = moderationController.streamKycImage(
                SecurityContextHolder.getContext().getAuthentication(),
                "v-01",
                "front",
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getCacheControl()).contains("no-store");

        // Asserts exactly 1 audit log was saved to the database
        assertThat(recordedAccessLogs).hasSize(1);
        KycAccessLog log = recordedAccessLogs.get(0);
        assertThat(log.getActorId()).isEqualTo("u-mod-01");
        assertThat(log.getActorRole()).isEqualTo("ROLE_MODERATOR");
        assertThat(log.getAction()).isEqualTo(KycAccessAction.VIEW_FRONT);
        assertThat(log.getIp()).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("3. Roles Authority: Verifies GrantedAuthorities for 5 roles")
    void testAllRolesAuthorities() {
        for (String role : List.of("TENANT", "LANDLORD", "MODERATOR", "ADMIN")) {
            authenticate("user-" + role, role);
            assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_" + role);
        }
    }
}
