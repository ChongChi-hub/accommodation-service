package vn.cozynest.identity.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.cozynest.identity.application.dto.AuthResult;
import vn.cozynest.identity.application.dto.RefreshTokenCommand;
import vn.cozynest.identity.application.port.TokenProviderPort;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.*;
import vn.cozynest.identity.domain.repository.LandlordProfileRepository;
import vn.cozynest.identity.domain.repository.RefreshTokenRepository;
import vn.cozynest.identity.domain.repository.UserRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefreshTokenUseCaseTest {

    private RefreshTokenRepository refreshTokenRepository;
    private UserRepository userRepository;
    private LandlordProfileRepository landlordProfileRepository;
    private TokenProviderPort tokenProvider;
    private RefreshTokenUseCase refreshTokenUseCase;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        userRepository = mock(UserRepository.class);
        landlordProfileRepository = mock(LandlordProfileRepository.class);
        tokenProvider = mock(TokenProviderPort.class);

        when(tokenProvider.hashToken("valid-refresh-token")).thenReturn("hash-123");
        when(tokenProvider.generateRefreshToken()).thenReturn("new-raw-refresh-token");
        when(tokenProvider.hashToken("new-raw-refresh-token")).thenReturn("new-token-hash");

        refreshTokenUseCase = new RefreshTokenUseCase(
                refreshTokenRepository, userRepository, landlordProfileRepository, tokenProvider
        );
    }

    @Test
    @DisplayName("R7: Refresh re-reads roles from DB; newly approved user receives token with LANDLORD")
    void testRefreshReReadsRolesFromDb() {
        RefreshToken validToken = RefreshToken.create(
                "user-123", "hash-123", "Web", "127.0.0.1", Instant.now().plusSeconds(3600)
        );
        when(refreshTokenRepository.findByTokenHash("hash-123")).thenReturn(Optional.of(validToken));

        // User now has both TENANT and LANDLORD in DB after approval
        User userInDb = new User(
                "user-123", "landlord@example.com", "0901234567", "hash", "Landlord User",
                null, null, null, UserStatus.ACTIVE, Instant.now(), null,
                Set.of(UserRoleType.TENANT, UserRoleType.LANDLORD), Instant.now(), Instant.now()
        );
        when(userRepository.findById("user-123")).thenReturn(Optional.of(userInDb));

        LandlordProfile approvedProfile = new LandlordProfile(
                "lp-123", "user-123", LandlordApprovalStatus.APPROVED, Instant.now(), Instant.now(),
                "admin-01", Instant.now(), null, null, 1L, Instant.now(), Instant.now()
        );
        when(landlordProfileRepository.findByUserId("user-123")).thenReturn(Optional.of(approvedProfile));
        when(tokenProvider.generateAccessToken(userInDb)).thenReturn("jwt-with-landlord-and-tenant");

        RefreshTokenCommand cmd = RefreshTokenCommand.builder()
                .refreshToken("valid-refresh-token")
                .ipAddress("127.0.0.1")
                .build();

        AuthResult result = refreshTokenUseCase.execute(cmd);

        assertThat(result.getToken()).isEqualTo("jwt-with-landlord-and-tenant");
        assertThat(result.getUser().getRoles()).contains("TENANT", "LANDLORD");
        assertThat(result.getRoles()).contains("TENANT", "LANDLORD");
        assertThat(result.getLandlordApplication().getStatus()).isEqualTo("APPROVED");

        verify(tokenProvider).generateAccessToken(userInDb);
    }

    @Test
    @DisplayName("Refresh rejected for SUSPENDED user with ERR_USER_SUSPENDED")
    void testRefreshSuspendedUserRejected() {
        RefreshToken validToken = RefreshToken.create(
                "user-123", "hash-123", "Web", "127.0.0.1", Instant.now().plusSeconds(3600)
        );
        when(refreshTokenRepository.findByTokenHash("hash-123")).thenReturn(Optional.of(validToken));

        User suspendedUser = new User(
                "user-123", "suspended@example.com", "0901234567", "hash", "Suspended User",
                null, null, null, UserStatus.SUSPENDED, Instant.now(), null,
                Set.of(UserRoleType.TENANT), Instant.now(), Instant.now()
        );
        when(userRepository.findById("user-123")).thenReturn(Optional.of(suspendedUser));

        RefreshTokenCommand cmd = RefreshTokenCommand.builder().refreshToken("valid-refresh-token").build();

        assertThatThrownBy(() -> refreshTokenUseCase.execute(cmd))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_USER_SUSPENDED");
    }

    @Test
    @DisplayName("Refresh rejected for DELETED user with ERR_USER_DELETED")
    void testRefreshDeletedUserRejected() {
        RefreshToken validToken = RefreshToken.create(
                "user-123", "hash-123", "Web", "127.0.0.1", Instant.now().plusSeconds(3600)
        );
        when(refreshTokenRepository.findByTokenHash("hash-123")).thenReturn(Optional.of(validToken));

        User deletedUser = new User(
                "user-123", "deleted@example.com", "0901234567", "hash", "Deleted User",
                null, null, null, UserStatus.DELETED, Instant.now(), null,
                Set.of(UserRoleType.TENANT), Instant.now(), Instant.now()
        );
        when(userRepository.findById("user-123")).thenReturn(Optional.of(deletedUser));

        RefreshTokenCommand cmd = RefreshTokenCommand.builder().refreshToken("valid-refresh-token").build();

        assertThatThrownBy(() -> refreshTokenUseCase.execute(cmd))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_USER_DELETED");
    }
}

