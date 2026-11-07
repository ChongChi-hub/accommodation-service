package vn.toam.identity.infrastructure.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.toam.common.dto.ApiResponse;
import vn.toam.identity.application.dto.InternalKycStatusResponse;
import vn.toam.identity.application.service.InternalUserService;
import vn.toam.identity.domain.model.KycStatus;
import vn.toam.identity.domain.repository.UserVerificationRepository;
import vn.toam.identity.interfaces.rest.InternalUserController;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KycSecurityMatrixTest {

    private InternalUserController internalUserController;
    private UserVerificationRepository verificationRepository;

    @BeforeEach
    void setUp() {
        verificationRepository = mock(UserVerificationRepository.class);
        InternalUserService internalService = new InternalUserService(verificationRepository);
        internalUserController = new InternalUserController(internalService, "ToAmInternalServiceKeySecret2026");
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Internal KYC status endpoint rejects requests without correct service key (403 Forbidden)")
    void testInternalEndpointRejectsWithoutKey() {
        ResponseEntity<ApiResponse<InternalKycStatusResponse>> response =
                internalUserController.getKycStatus("u-landlord-01", "wrong-secret-key");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("ERR_FORBIDDEN");
    }

    @Test
    @DisplayName("Internal KYC status endpoint succeeds with valid service key")
    void testInternalEndpointSucceedsWithValidKey() {
        when(verificationRepository.findByUserId("u-landlord-01")).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<InternalKycStatusResponse>> response =
                internalUserController.getKycStatus("u-landlord-01", "ToAmInternalServiceKeySecret2026");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getKycStatus()).isEqualTo(KycStatus.NONE.name());
        assertThat(response.getBody().getData().isApproved()).isFalse();
    }

    @Test
    @DisplayName("Security Context sets correct authorities for LANDLORD")
    void testLandlordSecurityContext() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "u-landlord-01", null, List.of(new SimpleGrantedAuthority("ROLE_LANDLORD"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_LANDLORD");
    }

    @Test
    @DisplayName("Security Context sets correct authorities for MODERATOR and ADMIN")
    void testModeratorAndAdminSecurityContext() {
        UsernamePasswordAuthenticationToken modAuth = new UsernamePasswordAuthenticationToken(
                "u-mod-01", null, List.of(new SimpleGrantedAuthority("ROLE_MODERATOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(modAuth);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_MODERATOR");

        UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken(
                "u-admin-01", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(adminAuth);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }
}

