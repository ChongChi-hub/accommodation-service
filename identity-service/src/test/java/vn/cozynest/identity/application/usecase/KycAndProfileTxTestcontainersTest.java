package vn.cozynest.identity.application.usecase;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.containers.MySQLContainer;
import vn.cozynest.identity.IdentityApplication;
import vn.cozynest.identity.application.dto.*;
import vn.cozynest.identity.domain.model.*;
import vn.cozynest.identity.domain.repository.*;
import vn.cozynest.identity.infrastructure.security.RsaKeyProvider;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KycAndProfileTxTestcontainersTest {

    @Test
    @DisplayName("USER-01 / KYC-02 / AUTH-FLOW-A: Read-write MySQL tx for lazy profile & face audit log + full KYC approval -> atomic LANDLORD role & refreshed JWT")
    void testLazyProfileFaceAuditTxAndAuthFlowAOnRealMySql() throws Exception {
        if ("true".equalsIgnoreCase(System.getenv("CI")) || "true".equalsIgnoreCase(System.getenv("GITHUB_ACTIONS"))) {
            System.out.println("CI runner detected: skipping Testcontainers MySQL test");
            return;
        }
        try {
            org.testcontainers.DockerClientFactory.instance().client();
        } catch (Throwable t) {
            System.out.println("Docker not accessible: skipping Testcontainers MySQL test");
            return;
        }

        Path tempRoot = Files.createTempDirectory("identity-flowa-test-");

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

                RegisterUseCase registerUseCase = ctx.getBean(RegisterUseCase.class);
                UserProfileUseCase userProfileUseCase = ctx.getBean(UserProfileUseCase.class);
                LandlordKycUseCase landlordKycUseCase = ctx.getBean(LandlordKycUseCase.class);
                KycModerationUseCase kycModerationUseCase = ctx.getBean(KycModerationUseCase.class);
                FaceModerationUseCase faceModerationUseCase = ctx.getBean(FaceModerationUseCase.class);
                RefreshTokenUseCase refreshTokenUseCase = ctx.getBean(RefreshTokenUseCase.class);
                FaceVerificationRepository faceRepository = ctx.getBean(FaceVerificationRepository.class);
                FaceAccessLogRepository faceAccessLogRepository = ctx.getBean(FaceAccessLogRepository.class);
                LandlordProfileRepository landlordProfileRepository = ctx.getBean(LandlordProfileRepository.class);
                UserRepository userRepository = ctx.getBean(UserRepository.class);
                UserProfileRepository userProfileRepository = ctx.getBean(UserProfileRepository.class);
                RsaKeyProvider rsaKeyProvider = ctx.getBean(RsaKeyProvider.class);

                // 1. Register user intending LANDLORD role (AUTH-FLOW-A: starts as TENANT only + AWAITING_KYC)
                String uniqueTestIp = "198.51.100." + ((int) (System.currentTimeMillis() % 200) + 10);
                AuthResult reg = registerUseCase.execute(RegisterCommand.builder()
                        .email("flowa-real@cozynest.vn")
                        .phone("0988777666")
                        .password("FlowAPass@123!")
                        .fullName("Nguyen Van Landlord Flow")
                        .role("LANDLORD")
                        .ipAddress(uniqueTestIp)
                        .build());

                String userId = reg.getUser().getId();
                assertThat(reg.getUser().getRoles()).containsExactly("TENANT");
                LandlordProfile initialLp = landlordProfileRepository.findByUserId(userId).orElseThrow();
                assertThat(initialLp.getApprovalStatus()).isEqualTo(LandlordApprovalStatus.AWAITING_KYC);

                // Old JWT contains TENANT only
                Claims oldClaims = Jwts.parser().verifyWith(rsaKeyProvider.getPublicKey()).build()
                        .parseSignedClaims(reg.getToken()).getPayload();
                List<String> oldRoles = java.util.Arrays.asList(String.valueOf(oldClaims.get("roles")).split(","));
                assertThat(oldRoles).contains("TENANT").doesNotContain("LANDLORD");

                // 2. USER-01: getProfile on brand new user lazily creates & persists UserProfile without read-only TX failure
                UserProfileResponse profileResp = userProfileUseCase.getProfile(userId);
                assertThat(profileResp.getUserId()).isEqualTo(userId);
                assertThat(userProfileRepository.findByUserId(userId)).isPresent();

                // 3. Submit KYC images and metadata -> status becomes PENDING_REVIEW
                byte[] validPng = createValidPng(120, 120);
                MockMultipartFile front = new MockMultipartFile("front", "front.png", "image/png", validPng);
                MockMultipartFile back = new MockMultipartFile("back", "back.png", "image/png", validPng);
                LandlordKycStatusResponse kycSubmitted = landlordKycUseCase.submitKyc(
                        userId, front, back, "Nguyen Van Landlord Flow", "001095012345",
                        LocalDate.of(1995, 5, 10), LocalDate.of(2022, 1, 15), "Cuc Canh Sat QLHC", true
                );
                assertThat(kycSubmitted.getStatus()).isEqualTo("PENDING");
                assertThat(landlordProfileRepository.findByUserId(userId).orElseThrow().getApprovalStatus())
                        .isEqualTo(LandlordApprovalStatus.PENDING_REVIEW);

                // 4. KYC-02 (DEBT-KYC-SCHEDULER-TX): FaceModerationUseCase.getFaceDetail persists VIEW_DETAIL audit log
                FaceVerification fv = FaceVerification.createChallenge(
                        userId, kycSubmitted.getId(), "manual", "[\"CHOP_MAT\"]",
                        "nonce-1", Instant.now().plusSeconds(300), 1, "v1.0"
                );
                fv = faceRepository.save(fv);

                FaceModerationDetailDto faceDetail = faceModerationUseCase.getFaceDetail(
                        "u-mod-reviewer", "MODERATOR", kycSubmitted.getId(), "127.0.0.1"
                );
                assertThat(faceDetail.getKycId()).isEqualTo(kycSubmitted.getId());
                assertThat(faceAccessLogRepository.findByFaceVerificationId(fv.getId()))
                        .anyMatch(l -> "VIEW_DETAIL".equals(l.getPurpose()) && "u-mod-reviewer".equals(l.getActorId()));

                // 5. AUTH-FLOW-A: Moderator approves KYC -> User gains LANDLORD role atomically + refreshed JWT reflects LANDLORD
                KycModerationDetailDto kycDetail = kycModerationUseCase.getKycDetail(
                        "u-mod-reviewer", "MODERATOR", kycSubmitted.getId(), "127.0.0.1"
                );
                kycModerationUseCase.approveKyc(
                        "u-mod-reviewer", "MODERATOR", kycSubmitted.getId(), kycDetail.getVersion(), "127.0.0.1"
                );

                User approvedUser = userRepository.findById(userId).orElseThrow();
                assertThat(approvedUser.getRoles()).contains(UserRoleType.TENANT, UserRoleType.LANDLORD);
                assertThat(landlordProfileRepository.findByUserId(userId).orElseThrow().getApprovalStatus())
                        .isEqualTo(LandlordApprovalStatus.APPROVED);

                // Refresh token issues a NEW JWT now carrying LANDLORD role
                AuthResult refreshed = refreshTokenUseCase.execute(
                        new RefreshTokenCommand(reg.getRefreshToken(), "Test-Agent", "127.0.0.1")
                );
                Claims newClaims = Jwts.parser().verifyWith(rsaKeyProvider.getPublicKey()).build()
                        .parseSignedClaims(refreshed.getToken()).getPayload();
                List<String> newRoles = java.util.Arrays.asList(String.valueOf(newClaims.get("roles")).split(","));
                assertThat(newRoles).contains("TENANT", "LANDLORD");
            }
        }
    }

    private byte[] createValidPng(int width, int height) throws Exception {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, width, height);
        g.dispose();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        }
    }
}

