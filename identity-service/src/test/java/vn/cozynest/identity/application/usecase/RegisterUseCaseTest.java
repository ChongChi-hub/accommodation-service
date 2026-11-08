package vn.cozynest.identity.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vn.cozynest.identity.application.dto.AuthResult;
import vn.cozynest.identity.application.dto.RegisterCommand;
import vn.cozynest.identity.application.port.PasswordEncoderPort;
import vn.cozynest.identity.application.port.RateLimiterPort;
import vn.cozynest.identity.application.port.TokenProviderPort;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.LandlordApprovalStatus;
import vn.cozynest.identity.domain.model.LandlordProfile;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.repository.LandlordProfileRepository;
import vn.cozynest.identity.domain.repository.RefreshTokenRepository;
import vn.cozynest.identity.domain.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RegisterUseCaseTest {

    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private LandlordProfileRepository landlordProfileRepository;
    private PasswordEncoderPort passwordEncoder;
    private TokenProviderPort tokenProvider;
    private RateLimiterPort rateLimiter;
    private RegisterUseCase registerUseCase;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        landlordProfileRepository = mock(LandlordProfileRepository.class);
        passwordEncoder = mock(PasswordEncoderPort.class);
        tokenProvider = mock(TokenProviderPort.class);
        rateLimiter = mock(RateLimiterPort.class);

        when(passwordEncoder.encode(any())).thenReturn("hashed_pwd");
        when(tokenProvider.generateAccessToken(any())).thenReturn("mock-access-token");
        when(tokenProvider.generateRefreshToken()).thenReturn("mock-refresh-token");
        when(tokenProvider.hashToken(any())).thenReturn("mock-token-hash");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        registerUseCase = new RegisterUseCase(
                userRepository, refreshTokenRepository, landlordProfileRepository, passwordEncoder, tokenProvider, rateLimiter
        );
    }

    @Test
    @DisplayName("R1: Register as TENANT -> Receives TENANT role only and no LandlordProfile")
    void testRegisterAsTenant() {
        RegisterCommand cmd = RegisterCommand.builder()
                .email("tenant.new@example.com")
                .phone("0901111222")
                .password("Password@123")
                .fullName("Tenant New")
                .role("TENANT")
                .ipAddress("127.0.0.1")
                .build();

        AuthResult result = registerUseCase.execute(cmd);

        assertThat(result.getUser().getRoles()).containsExactly("TENANT");
        assertThat(result.getRoles()).containsExactly("TENANT");
        assertThat(result.getLandlordApplication()).isNull();
        verify(landlordProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("R2: Register as LANDLORD -> Receives TENANT role only and creates AWAITING_KYC LandlordProfile")
    void testRegisterAsLandlordCreatesAwaitingKyc() {
        RegisterCommand cmd = RegisterCommand.builder()
                .email("landlord.new@example.com")
                .phone("0902222333")
                .password("Password@123")
                .fullName("Landlord New")
                .role("LANDLORD")
                .ipAddress("127.0.0.1")
                .build();

        when(landlordProfileRepository.save(any(LandlordProfile.class))).thenAnswer(i -> i.getArgument(0));

        AuthResult result = registerUseCase.execute(cmd);

        // Crucial: Role must be TENANT, NOT LANDLORD!
        assertThat(result.getUser().getRoles()).containsExactly("TENANT");
        assertThat(result.getRoles()).containsExactly("TENANT");
        assertThat(result.getLandlordApplication()).isNotNull();
        assertThat(result.getLandlordApplication().getStatus()).isEqualTo(LandlordApprovalStatus.AWAITING_KYC.name());

        ArgumentCaptor<LandlordProfile> profileCaptor = ArgumentCaptor.forClass(LandlordProfile.class);
        verify(landlordProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getApprovalStatus()).isEqualTo(LandlordApprovalStatus.AWAITING_KYC);
    }

    @Test
    @DisplayName("Mass-assignment attack: role=ADMIN is rejected with ERR_AUTH_FORBIDDEN_ROLE")
    void testMassAssignmentAdminRejected() {
        RegisterCommand cmd = RegisterCommand.builder()
                .email("hacker@example.com")
                .phone("0909999888")
                .password("Password@123")
                .fullName("Hacker")
                .role("ADMIN")
                .ipAddress("127.0.0.1")
                .build();

        assertThatThrownBy(() -> registerUseCase.execute(cmd))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_AUTH_FORBIDDEN_ROLE");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Duplicate email returns ERR_AUTH_EMAIL_EXISTS")
    void testDuplicateEmail() {
        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        RegisterCommand cmd = RegisterCommand.builder()
                .email("duplicate@example.com")
                .phone("0901234567")
                .password("Password@123")
                .fullName("Duplicate")
                .role("TENANT")
                .build();

        assertThatThrownBy(() -> registerUseCase.execute(cmd))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_AUTH_EMAIL_EXISTS");
    }

    @Test
    @DisplayName("Duplicate phone returns ERR_AUTH_PHONE_EXISTS")
    void testDuplicatePhone() {
        when(userRepository.existsByPhone("+84901234567")).thenReturn(true);

        RegisterCommand cmd = RegisterCommand.builder()
                .email("unique@example.com")
                .phone("0901234567")
                .password("Password@123")
                .fullName("Unique")
                .role("TENANT")
                .build();

        assertThatThrownBy(() -> registerUseCase.execute(cmd))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_AUTH_PHONE_EXISTS");
    }
}

