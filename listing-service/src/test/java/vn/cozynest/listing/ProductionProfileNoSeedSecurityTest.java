package vn.cozynest.listing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionProfileNoSeedSecurityTest {

    private Path findDir(String relative) {
        Path direct = Paths.get(relative);
        if (Files.exists(direct)) return direct;
        Path sub = Paths.get("listing-service", relative);
        if (Files.exists(sub)) return sub;
        throw new IllegalStateException("Could not find path: " + relative);
    }

    @Test
    @DisplayName("Verify listing-service Flyway migration directory has 0 seed files and 0 demo listings")
    void testMigrationPathHasNoDemoListings() throws IOException {
        Path migrationDir = findDir("src/main/resources/db/migration");
        try (Stream<Path> files = Files.list(migrationDir)) {
            List<Path> migrationFiles = files.toList();

            for (Path file : migrationFiles) {
                String filename = file.getFileName().toString();
                assertThat(filename)
                        .withFailMessage("Migration directory must not contain seed files: %s", filename)
                        .doesNotContainIgnoringCase("seed");

                String content = Files.readString(file);
                assertThat(content)
                        .withFailMessage("Production migration %s must not insert demo landlord rooms", filename)
                        .doesNotContain("demo_landlord_room");
            }
        }
    }

    @Test
    @DisplayName("Verify Default Application Configuration points Flyway strictly to db/migration without db/seed")
    void testDefaultConfigHasNoSeedLocation() throws IOException {
        Path appYmlPath = findDir("src/main/resources/application.yml");
        String content = Files.readString(appYmlPath);

        assertThat(content)
                .withFailMessage("Default production config must have flyway locations pointing strictly to db/migration")
                .contains("locations: classpath:db/migration");

        assertThat(content)
                .withFailMessage("Default production config must NEVER load classpath:db/seed")
                .doesNotContain("classpath:db/seed");
    }

    @Test
    @DisplayName("Verify All Microservices isolate seed scripts into db/seed and out of db/migration")
    void testAllServicesIsolateSeeds() throws IOException {
        String[] services = {"identity-service", "listing-service", "billing-service", "booking-service", "communication-service", "admin-service"};
        Path root = Paths.get(".").toAbsolutePath().normalize();
        if (!Files.exists(root.resolve("listing-service"))) {
            root = root.getParent();
        }

        for (String service : services) {
            Path serviceMigration = root.resolve(service).resolve("src/main/resources/db/migration");
            if (Files.exists(serviceMigration)) {
                try (Stream<Path> files = Files.list(serviceMigration)) {
                    for (Path file : files.toList()) {
                        String name = file.getFileName().toString();
                        assertThat(name)
                                .withFailMessage("Service %s contains seed file in migration directory: %s", service, name)
                                .doesNotContainIgnoringCase("seed");
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Verify SeedAssetLoader is guarded from prod and application-prod.yml disables Swagger")
    void testSeedAssetLoaderAndProdConfig() throws IOException {
        Path prodYml = findDir("src/main/resources/application-prod.yml");
        assertThat(Files.exists(prodYml)).isTrue();
        String prodContent = Files.readString(prodYml);
        assertThat(prodContent).contains("enabled: false");

        Path seedAssetLoader = findDir("src/main/java/vn/cozynest/listing/infrastructure/storage/SeedAssetLoader.java");
        assertThat(Files.readString(seedAssetLoader)).contains("@Profile");
    }
}
