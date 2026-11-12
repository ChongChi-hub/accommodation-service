package vn.cozynest.identity.application.usecase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.MySQLContainer;
import vn.cozynest.identity.IdentityApplication;
import vn.cozynest.identity.application.dto.AuthResult;
import vn.cozynest.identity.application.dto.RefreshTokenCommand;
import vn.cozynest.identity.application.dto.RegisterCommand;
import vn.cozynest.identity.application.port.TokenProviderPort;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.model.RefreshToken;
import vn.cozynest.identity.domain.repository.RefreshTokenRepository;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenReuseTestcontainersTest {

    @Test
    @DisplayName("AUTH-02: Replay of rotated refresh token commits revokeAllByUserId despite throwing IdentityDomainException, and logout commits revocation")
    void testReuseDetectionCommitsRevokeAllAndLogoutRevokesTokensInRealMySql() throws Exception {
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

        Path tempRoot = Files.createTempDirectory("identity-rt-test-");

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
                RefreshTokenUseCase refreshTokenUseCase = ctx.getBean(RefreshTokenUseCase.class);
                LogoutUseCase logoutUseCase = ctx.getBean(LogoutUseCase.class);
                RefreshTokenRepository refreshRepo = ctx.getBean(RefreshTokenRepository.class);
                TokenProviderPort tokenProvider = ctx.getBean(TokenProviderPort.class);

                // 1. Register -> initial refresh token R1
                AuthResult reg = registerUseCase.execute(RegisterCommand.builder()
                        .email("rt.reuse@cozynest.vn")
                        .phone("0912000111")
                        .password("CozyStrong@2026")
                        .fullName("Token Tester")
                        .role("TENANT")
                        .ipAddress("127.0.0.1")
                        .build());

                String rt1 = reg.getRefreshToken();
                assertThat(rt1).isNotBlank();

                // 2. Rotate R1 -> R2
                AuthResult rotated = refreshTokenUseCase.execute(RefreshTokenCommand.builder()
                        .refreshToken(rt1)
                        .ipAddress("127.0.0.1")
                        .deviceInfo("JUnit")
                        .build());
                String rt2 = rotated.getRefreshToken();
                assertThat(rt2).isNotBlank().isNotEqualTo(rt1);

                // 3. Reuse R1 -> MUST throw ERR_AUTH_TOKEN_EXPIRED AND revoke R2 in real DB (not rolled back!)
                assertThatThrownBy(() -> refreshTokenUseCase.execute(RefreshTokenCommand.builder()
                        .refreshToken(rt1)
                        .ipAddress("127.0.0.1")
                        .deviceInfo("Attacker")
                        .build()))
                        .isInstanceOf(IdentityDomainException.class)
                        .extracting("errorCode").isEqualTo("ERR_AUTH_TOKEN_EXPIRED");

                RefreshToken rt2AfterReuse = refreshRepo.findByTokenHash(tokenProvider.hashToken(rt2)).orElseThrow();
                assertThat(rt2AfterReuse.getRevokedAt()).isNotNull();

                // 4. Subsequent attempt with R2 must fail as well
                assertThatThrownBy(() -> refreshTokenUseCase.execute(RefreshTokenCommand.builder()
                        .refreshToken(rt2)
                        .ipAddress("127.0.0.1")
                        .build()))
                        .isInstanceOf(IdentityDomainException.class);

                // 5. Register/Login second user and test LogoutUseCase transactional revocation
                AuthResult reg2 = registerUseCase.execute(RegisterCommand.builder()
                        .email("logout.test@cozynest.vn")
                        .phone("0912000222")
                        .password("CozyStrong@2026")
                        .fullName("Logout Tester")
                        .role("TENANT")
                        .ipAddress("127.0.0.1")
                        .build());
                String rtLogout = reg2.getRefreshToken();
                logoutUseCase.execute(reg2.getUser().getId(), null);
                RefreshToken afterLogout = refreshRepo.findByTokenHash(tokenProvider.hashToken(rtLogout)).orElseThrow();
                assertThat(afterLogout.getRevokedAt()).isNotNull();
            }
        }
    }
}

