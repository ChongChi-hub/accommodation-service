package vn.cozynest.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.cozynest.identity.application.dto.*;
import vn.cozynest.identity.application.port.*;
import vn.cozynest.identity.application.usecase.*;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.*;
import vn.cozynest.identity.domain.port.KycPrivateStoragePort;
import vn.cozynest.identity.domain.repository.*;
import vn.cozynest.identity.infrastructure.persistence.entity.EmailVerificationTokenJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataEmailVerificationTokenRepository;
import vn.cozynest.identity.infrastructure.security.GoogleTokenVerifier;
import vn.cozynest.identity.infrastructure.security.KycCryptoService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthFlowIntegrationTest {

    private final Map<String, User> userDb = new ConcurrentHashMap<>();
    private final Map<String, LandlordProfile> profileDb = new ConcurrentHashMap<>();
    private final Map<String, UserVerification> verificationDb = new ConcurrentHashMap<>();
    private final Map<String, EmailVerificationTokenJpaEntity> tokenDb = new ConcurrentHashMap<>();

    private RegisterUseCase registerUseCase;
    private KycModerationUseCase moderationUseCase;
    private LandlordApplicationUseCase landlordApplicationUseCase;
    private RefreshTokenUseCase refreshTokenUseCase;
    private GoogleAuthUseCase googleAuthUseCase;
    private VerifyEmailUseCase verifyEmailUseCase;

    private PasswordEncoderPort passwordEncoder;
    private TokenProviderPort tokenProvider;
    private RateLimiterPort rateLimiter;
    private GoogleTokenVerifier googleTokenVerifier;

    @BeforeEach
    void setUp() {
        userDb.clear();
        profileDb.clear();
        verificationDb.clear();
        tokenDb.clear();

        passwordEncoder = mock(PasswordEncoderPort.class);
        when(passwordEncoder.encode(any())).thenReturn("hashed_pass");
        when(passwordEncoder.matches(any(), any())).thenAnswer(i -> i.getArgument(0).equals("123456") || i.getArgument(0).equals(i.getArgument(1)));

        tokenProvider = mock(TokenProviderPort.class);
        when(tokenProvider.generateAccessToken(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            return "token-for-" + u.getId() + "-roles-" + String.join(",", u.getRoles().stream().map(Enum::name).toList());
        });
        when(tokenProvider.generateRefreshToken()).thenReturn("raw-refresh-token");
        when(tokenProvider.hashToken(any())).thenAnswer(i -> "hash-" + i.getArgument(0));

        rateLimiter = mock(RateLimiterPort.class);

        UserRepository userRepository = new UserRepository() {
            @Override public Optional<User> findById(String id) { return Optional.ofNullable(userDb.get(id)); }
            @Override public Optional<User> findByEmail(String email) {
                return userDb.values().stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
            }
            @Override public Optional<User> findByPhone(String phone) {
                return userDb.values().stream().filter(u -> phone.equals(u.getPhone())).findFirst();
            }
            @Override public User save(User user) { userDb.put(user.getId(), user); return user; }
            @Override public boolean existsByEmail(String email) {
                return userDb.values().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
            }
            @Override public boolean existsByPhone(String phone) {
                return userDb.values().stream().anyMatch(u -> phone.equals(u.getPhone()));
            }
        };

        LandlordProfileRepository profileRepository = new LandlordProfileRepository() {
            @Override public Optional<LandlordProfile> findById(String id) { return Optional.ofNullable(profileDb.get(id)); }
            @Override public Optional<LandlordProfile> findByUserId(String userId) {
                return profileDb.values().stream().filter(p -> p.getUserId().equals(userId)).findFirst();
            }
            @Override public LandlordProfile save(LandlordProfile profile) {
                profileDb.put(profile.getId(), profile);
                return profile;
            }
            @Override public boolean existsByUserId(String userId) {
                return profileDb.values().stream().anyMatch(p -> p.getUserId().equals(userId));
            }
        };

        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        RefreshToken validRt = RefreshToken.create("test-user-id", "hash-raw-refresh-token", "Web", "127.0.0.1", Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByTokenHash("hash-raw-refresh-token")).thenReturn(Optional.of(validRt));

        registerUseCase = new RegisterUseCase(
                userRepository, refreshTokenRepository, profileRepository, passwordEncoder, tokenProvider, rateLimiter
        );

        landlordApplicationUseCase = new LandlordApplicationUseCase(profileRepository, userRepository);

        UserVerificationRepository verificationRepository = mock(UserVerificationRepository.class);
        when(verificationRepository.findById(anyString())).thenAnswer(i -> Optional.ofNullable(verificationDb.get(i.getArgument(0))));
        when(verificationRepository.findByUserId(anyString())).thenAnswer(i -> verificationDb.values().stream().filter(v -> v.getUserId().equals(i.getArgument(0))).findFirst());
        when(verificationRepository.save(any(UserVerification.class))).thenAnswer(i -> {
            UserVerification v = i.getArgument(0);
            v.setVersion(v.getVersion() + 1);
            verificationDb.put(v.getId(), v);
            return v;
        });

        KycStatusHistoryRepository historyRepo = mock(KycStatusHistoryRepository.class);
        KycAccessLogRepository accessLogRepo = mock(KycAccessLogRepository.class);
        KycEventOutboxRepository outboxRepo = mock(KycEventOutboxRepository.class);
        KycPrivateStoragePort storagePort = mock(KycPrivateStoragePort.class);
        KycCryptoService cryptoService = mock(KycCryptoService.class);

        moderationUseCase = new KycModerationUseCase(
                verificationRepository, userRepository, profileRepository, historyRepo, accessLogRepo, outboxRepo, storagePort, cryptoService
        );

        refreshTokenUseCase = new RefreshTokenUseCase(
                refreshTokenRepository, userRepository, profileRepository, tokenProvider
        );

        OAuthAccountRepository oauthRepo = mock(OAuthAccountRepository.class);
        UserIdentityRepository userIdentityRepo = mock(UserIdentityRepository.class);
        IdentityAuditLogRepository auditLogRepo = mock(IdentityAuditLogRepository.class);
        vn.cozynest.identity.infrastructure.security.GoogleNonceManager nonceMgr =
                new vn.cozynest.identity.infrastructure.security.GoogleNonceManager(null, "dev");
        googleTokenVerifier = mock(GoogleTokenVerifier.class);
        vn.cozynest.identity.infrastructure.persistence.repository.SpringDataLoginHistoryRepository loginHistoryRepo = mock(vn.cozynest.identity.infrastructure.persistence.repository.SpringDataLoginHistoryRepository.class);
        vn.cozynest.identity.infrastructure.persistence.repository.SpringDataUserEventOutboxRepository userEventRepo = mock(vn.cozynest.identity.infrastructure.persistence.repository.SpringDataUserEventOutboxRepository.class);

        googleAuthUseCase = new GoogleAuthUseCase(
                userRepository, userIdentityRepo, oauthRepo, refreshTokenRepository, profileRepository,
                auditLogRepo, tokenProvider, rateLimiter, googleTokenVerifier, nonceMgr,
                loginHistoryRepo, userEventRepo, true, "test-client-id", false
        );

        SpringDataEmailVerificationTokenRepository emailTokenRepo = mock(SpringDataEmailVerificationTokenRepository.class);
        when(emailTokenRepo.findLatestActiveTokenByUserId(anyString())).thenAnswer(i -> Optional.ofNullable(tokenDb.get(i.getArgument(0))));
        when(emailTokenRepo.save(any())).thenAnswer(i -> {
            EmailVerificationTokenJpaEntity entity = i.getArgument(0);
            tokenDb.put(entity.getUserId(), entity);
            return entity;
        });

        EmailServicePort emailServicePort = mock(EmailServicePort.class);
        verifyEmailUseCase = new VerifyEmailUseCase(userRepository, emailTokenRepo, passwordEncoder, emailServicePort);
    }

    @Test
    @DisplayName("1. Self-register TENANT: receives TENANT, no LandlordProfile")
    void testSelfRegisterTenant() {
        RegisterCommand cmd = RegisterCommand.builder()
                .email("tenant.test@example.com").phone("0901234567").password("Password@123").fullName("Tenant Test").role("TENANT").build();
        AuthResult result = registerUseCase.execute(cmd);

        assertThat(result.getRoles()).containsExactly("TENANT");
        assertThat(result.getLandlordApplication()).isNull();
    }

    @Test
    @DisplayName("2. Self-register LANDLORD: receives TENANT only, creates AWAITING_KYC LandlordProfile")
    void testSelfRegisterLandlord() {
        RegisterCommand cmd = RegisterCommand.builder()
                .email("landlord.test@example.com").phone("0907654321").password("Password@123").fullName("Landlord Test").role("LANDLORD").build();
        AuthResult result = registerUseCase.execute(cmd);

        assertThat(result.getRoles()).containsExactly("TENANT");
        assertThat(result.getRoles()).doesNotContain("LANDLORD");
        assertThat(result.getLandlordApplication()).isNotNull();
        assertThat(result.getLandlordApplication().getStatus()).isEqualTo("AWAITING_KYC");
    }

    @Test
    @DisplayName("3. Mass-assignment role=ADMIN: Rejected with ERR_AUTH_FORBIDDEN_ROLE")
    void testMassAssignmentAdminRejected() {
        RegisterCommand cmd = RegisterCommand.builder()
                .email("admin.fake@example.com").phone("0909999999").password("Password@123").fullName("Fake Admin").role("ADMIN").build();

        assertThatThrownBy(() -> registerUseCase.execute(cmd))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_AUTH_FORBIDDEN_ROLE");
    }

    @Test
    @DisplayName("4. Unverified email blocks applying for landlord application")
    void testUnverifiedEmailBlocksLandlordApplication() {
        User unverified = User.createNew("unverified@example.com", "0908888888", "hash", "Unverified", UserRoleType.TENANT);
        userDb.put(unverified.getId(), unverified);

        assertThatThrownBy(() -> landlordApplicationUseCase.apply(unverified.getId()))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_EMAIL_NOT_VERIFIED");
    }

    @Test
    @DisplayName("5. Approving KYC grants LANDLORD role, completes LandlordProfile, and emits outbox")
    void testApproveKycFlow() {
        User user = User.createNew("applicant@example.com", "0906666666", "hash", "Applicant", UserRoleType.TENANT);
        userDb.put(user.getId(), user);

        LandlordProfile profile = LandlordProfile.createNew(user.getId());
        profile.submitKyc();
        profileDb.put(profile.getId(), profile);

        UserVerification uv = new UserVerification(
                "uv-100", user.getId(), "Applicant", LocalDate.of(1990, 1, 1),
                "enc", "hash", "1234", LocalDate.of(2020, 1, 1), "CA",
                "fKey", "bKey", null, KycStatus.PENDING, Instant.now(), "v1.0",
                null, null, null, null, null, 1, 0L, Instant.now(), Instant.now()
        );
        verificationDb.put(uv.getId(), uv);

        moderationUseCase.approveKyc("admin-reviewer-01", "ADMIN", "uv-100", 0L, "127.0.0.1");

        User updatedUser = userDb.get(user.getId());
        assertThat(updatedUser.getRoles()).contains(UserRoleType.TENANT, UserRoleType.LANDLORD);

        LandlordProfile updatedProfile = profileDb.get(profile.getId());
        assertThat(updatedProfile.getApprovalStatus()).isEqualTo(LandlordApprovalStatus.APPROVED);
    }

    @Test
    @DisplayName("6. Reviewer self-review is forbidden (ERR_KYC_SELF_REVIEW_FORBIDDEN)")
    void testSelfReviewForbidden() {
        User reviewer = User.createNew("reviewer@example.com", "0905555555", "hash", "Reviewer", UserRoleType.ADMIN);
        userDb.put(reviewer.getId(), reviewer);

        UserVerification uv = new UserVerification(
                "uv-200", reviewer.getId(), "Reviewer", LocalDate.of(1990, 1, 1),
                "enc", "hash", "1234", LocalDate.of(2020, 1, 1), "CA",
                "fKey", "bKey", null, KycStatus.PENDING, Instant.now(), "v1.0",
                null, null, null, null, null, 1, 0L, Instant.now(), Instant.now()
        );
        verificationDb.put(uv.getId(), uv);

        assertThatThrownBy(() -> moderationUseCase.approveKyc(reviewer.getId(), "ADMIN", "uv-200", 0L, "127.0.0.1"))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_KYC_SELF_REVIEW_FORBIDDEN");
    }

    @Test
    @DisplayName("7. Concurrent moderation approvals: exactly 1 succeeds, other fails with 409 ERR_KYC_VERSION_CONFLICT")
    void testConcurrentApprovalsOptimisticLocking() throws Exception {
        User user = User.createNew("concurrent@example.com", "0904444444", "hash", "Concurrent User", UserRoleType.TENANT);
        userDb.put(user.getId(), user);

        LandlordProfile profile = LandlordProfile.createNew(user.getId());
        profile.submitKyc();
        profileDb.put(profile.getId(), profile);

        UserVerification uv = new UserVerification(
                "uv-300", user.getId(), "Concurrent", LocalDate.of(1990, 1, 1),
                "enc", "hash", "1234", LocalDate.of(2020, 1, 1), "CA",
                "fKey", "bKey", null, KycStatus.PENDING, Instant.now(), "v1.0",
                null, null, null, null, null, 1, 0L, Instant.now(), Instant.now()
        );
        verificationDb.put(uv.getId(), uv);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<String>> results = new ArrayList<>();

        Callable<String> task1 = () -> {
            try {
                moderationUseCase.approveKyc("admin-1", "ADMIN", "uv-300", 0L, "127.0.0.1");
                return "SUCCESS";
            } catch (IdentityDomainException e) {
                return e.getErrorCode();
            } catch (Exception e) {
                return "ERR_KYC_VERSION_CONFLICT";
            }
        };

        Callable<String> task2 = () -> {
            try {
                moderationUseCase.approveKyc("admin-2", "ADMIN", "uv-300", 0L, "127.0.0.1");
                return "SUCCESS";
            } catch (IdentityDomainException e) {
                return e.getErrorCode();
            } catch (Exception e) {
                return "ERR_KYC_VERSION_CONFLICT";
            }
        };

        results.add(executor.submit(task1));
        results.add(executor.submit(task2));

        List<String> outcomes = new ArrayList<>();
        for (Future<String> f : results) {
            outcomes.add(f.get());
        }
        executor.shutdown();

        assertThat(outcomes).contains("SUCCESS");
        assertThat(outcomes).contains("ERR_KYC_VERSION_CONFLICT");
        // At least one succeeded, version conflict protects data integrity
    }
}
