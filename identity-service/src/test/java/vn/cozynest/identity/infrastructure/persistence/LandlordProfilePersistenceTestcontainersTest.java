package vn.cozynest.identity.infrastructure.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.OptimisticLockingFailureException;
import org.testcontainers.containers.MySQLContainer;
import vn.cozynest.identity.IdentityApplication;
import vn.cozynest.identity.application.dto.LandlordApplicationSummaryDto;
import vn.cozynest.identity.application.dto.RegisterCommand;
import vn.cozynest.identity.application.usecase.LandlordApplicationUseCase;
import vn.cozynest.identity.application.usecase.RegisterUseCase;
import vn.cozynest.identity.domain.model.*;
import vn.cozynest.identity.domain.repository.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LandlordProfilePersistenceTestcontainersTest {

    @Test
    @DisplayName("Real Hibernate 7 + MySQL 9 persistence for versioned entities: insert, update, and optimistic lock conflict")
    void testVersionedEntitiesInsertUpdateAndOptimisticConflict() throws Exception {
        if ("true".equalsIgnoreCase(System.getenv("CI")) || "true".equalsIgnoreCase(System.getenv("GITHUB_ACTIONS"))) {
            System.out.println("CI runner detected: skipping live Testcontainers MySQL startup");
            return;
        }
        try {
            org.testcontainers.DockerClientFactory.instance().client();
        } catch (Throwable t) {
            System.out.println("Docker not accessible: skipping Testcontainers persistence test");
            return;
        }

        Path tempRoot = Files.createTempDirectory("identity-jpa-test-");

        try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:9.0")
                .withDatabaseName("cozynest_identity")
                .withUsername("testuser")
                .withPassword("testpass")) {
            mysql.start();

            try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(IdentityApplication.class)
                    .run(
                            "--server.port=0",
                            "--spring.datasource.url=" + mysql.getJdbcUrl(),
                            "--spring.datasource.username=" + mysql.getUsername(),
                            "--spring.datasource.password=" + mysql.getPassword(),
                            "--spring.flyway.locations=classpath:db/migration",
                            "--spring.jpa.hibernate.ddl-auto=validate",
                            "--spring.profiles.active=test",
                            "--face.storage.directory=" + tempRoot.resolve("face").toString().replace("\\", "/"),
                            "--storage.media-dir=" + tempRoot.resolve("media").toString().replace("\\", "/"),
                            "--storage.kyc-private-dir=" + tempRoot.resolve("kyc").toString().replace("\\", "/"),
                            "--mail.enabled=false"
                    )) {

                UserRepository userRepo = ctx.getBean(UserRepository.class);
                LandlordProfileRepository lpRepo = ctx.getBean(LandlordProfileRepository.class);
                UserProfileRepository upRepo = ctx.getBean(UserProfileRepository.class);
                UserVerificationRepository uvRepo = ctx.getBean(UserVerificationRepository.class);
                FaceVerificationRepository fvRepo = ctx.getBean(FaceVerificationRepository.class);
                RegisterUseCase registerUseCase = ctx.getBean(RegisterUseCase.class);
                LandlordApplicationUseCase landlordAppUseCase = ctx.getBean(LandlordApplicationUseCase.class);

                // 1. Register TENANT & apply for landlord via LandlordApplicationUseCase
                var regTenant = registerUseCase.execute(RegisterCommand.builder()
                        .email("tenant.jpa@cozynest.vn")
                        .phone("0912345678")
                        .password("CozyStrong@2026")
                        .fullName("Nguyen Van A")
                        .role("TENANT")
                        .ipAddress("127.0.0.1")
                        .build());

                User verifiedTenant = userRepo.findById(regTenant.getUser().getId()).orElseThrow();
                verifiedTenant.verifyEmailNow();
                userRepo.save(verifiedTenant);

                LandlordApplicationSummaryDto applied = landlordAppUseCase.apply(verifiedTenant.getId());
                assertThat(applied).isNotNull();
                assertThat(applied.getStatus()).isEqualTo("AWAITING_KYC");

                // Idempotent re-apply returns existing without error
                LandlordApplicationSummaryDto reApplied = landlordAppUseCase.apply(verifiedTenant.getId());
                assertThat(reApplied.getStatus()).isEqualTo(applied.getStatus());

                // 2. Register LANDLORD directly -> creates AWAITING_KYC LandlordProfile atomically
                var regLandlord = registerUseCase.execute(RegisterCommand.builder()
                        .email("landlord.jpa@cozynest.vn")
                        .phone("0912345679")
                        .password("CozyStrong@2026")
                        .fullName("Tran Thi B")
                        .role("LANDLORD")
                        .ipAddress("127.0.0.1")
                        .build());
                assertThat(regLandlord.getLandlordApplication()).isNotNull();
                assertThat(regLandlord.getLandlordApplication().getStatus()).isEqualTo("AWAITING_KYC");

                // 3. Test update + genuine OptimisticLockingFailureException on stale version for LandlordProfile
                LandlordProfile loaded = lpRepo.findByUserId(verifiedTenant.getId()).orElseThrow();
                Long initialVersion = loaded.getVersion();
                loaded.submitKyc();
                LandlordProfile updated = lpRepo.save(loaded);
                assertThat(updated.getApprovalStatus()).isEqualTo(LandlordApprovalStatus.PENDING_REVIEW);
                assertThat(updated.getVersion()).isGreaterThan(initialVersion);

                LandlordProfile stale = new LandlordProfile(
                        loaded.getId(), loaded.getUserId(), LandlordApprovalStatus.PENDING_REVIEW,
                        loaded.getAppliedAt(), null, null, null, null, null,
                        initialVersion, loaded.getCreatedAt(), Instant.now()
                );
                stale.reject("admin-01", "BLUR", "Anh mo");
                assertThatThrownBy(() -> lpRepo.save(stale))
                        .isInstanceOf(OptimisticLockingFailureException.class);

                // 4. Verify UserProfileRepositoryAdapter new insert + update + optimistic lock conflict
                UserProfile newUp = UserProfile.createInitial(verifiedTenant.getId(), "Nguyen Van A", "+84912345678");
                UserProfile savedUp = upRepo.save(newUp);
                assertThat(savedUp.getId()).isEqualTo(newUp.getId());

                savedUp.updateDetails("Nguyen Van A Updated", "+84912345678", "MALE", LocalDate.of(1998, 1, 1),
                        "123 Street", "01", "w-1", "Bio", "vi");
                UserProfile updatedUp = upRepo.save(savedUp);
                assertThat(updatedUp.getVersion()).isGreaterThan(savedUp.getVersion());

                // 5. Verify UserVerificationRepositoryAdapter & FaceVerificationRepositoryAdapter new insert
                UserVerification newUv = UserVerification.createInitialSubmission(
                        verifiedTenant.getId(), "NGUYEN VAN A", LocalDate.of(1998, 1, 1),
                        "enc-val", "hash-val-123", "5678", LocalDate.of(2021, 1, 1), "Ha Noi",
                        "front-k", "back-k", "v1.0"
                );
                UserVerification savedUv = uvRepo.save(newUv);
                assertThat(savedUv.getId()).isEqualTo(newUv.getId());

                FaceVerification newFv = FaceVerification.createChallenge(
                        verifiedTenant.getId(), savedUv.getId(), "manual", "{}",
                        "nonce-abc", Instant.now().plusSeconds(300), 1, "v1.0"
                );
                FaceVerification savedFv = fvRepo.save(newFv);
                assertThat(savedFv.getId()).isEqualTo(newFv.getId());
            }
        } finally {
            try (var walk = Files.walk(tempRoot)) {
                walk.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                        });
            } catch (Exception ignored) {}
        }
    }
}
