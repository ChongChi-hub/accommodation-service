package vn.cozynest.identity;

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
        Path sub = Paths.get("identity-service", relative);
        if (Files.exists(sub)) return sub;
        throw new IllegalStateException("Could not find path: " + relative);
    }

    @Test
    @DisplayName("Verify Flyway migration directory has 0 seed files and 0 demo user inserts")
    void testMigrationPathHasNoDemoAccounts() throws IOException {
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
                        .withFailMessage("Production migration %s must not insert demo admin@cozynest.vn", filename)
                        .doesNotContain("admin@cozynest.vn");
                assertThat(content)
                        .withFailMessage("Production migration %s must not insert demo password123", filename)
                        .doesNotContain("password123");
                assertThat(content)
                        .withFailMessage("Production migration %s must not insert demo tenant@cozynest.vn", filename)
                        .doesNotContain("tenant@cozynest.vn");
                assertThat(content)
                        .withFailMessage("Production migration %s must not insert demo landlord@cozynest.vn", filename)
                        .doesNotContain("landlord@cozynest.vn");
            }
        }
    }

    @Test
    @DisplayName("Verify Default Application Configuration points Flyway strictly to db/migration without db/seed")
    void testDefaultConfigHasNoSeedLocation() throws IOException {
        Path appYmlPath = findDir("src/main/resources/application.yml");
        String content = Files.readString(appYmlPath);

        // Default / Production config must strictly point to db/migration
        assertThat(content).contains("locations: classpath:db/migration");
        assertThat(content).doesNotContain("classpath:db/seed");
    }

    @Test
    @DisplayName("Verify Dev and Demo Profile Configurations explicitly enable db/seed")
    void testDevProfileEnablesSeedLocations() throws IOException {
        Path devYmlPath = findDir("src/main/resources/application-dev.yml");
        String devContent = Files.readString(devYmlPath);
        assertThat(devContent).contains("locations: classpath:db/migration,classpath:db/seed");

        Path demoYmlPath = findDir("src/main/resources/application-demo.yml");
        String demoContent = Files.readString(demoYmlPath);
        assertThat(demoContent).contains("locations: classpath:db/migration,classpath:db/seed");
    }

    @Test
    @DisplayName("Verify Seed directory exists and contains repeatable migration R__01_seed_demo_users")
    void testSeedDirectoryContainsRepeatableSeeds() throws IOException {
        Path seedDir = findDir("src/main/resources/db/seed");
        try (Stream<Path> files = Files.list(seedDir)) {
            List<String> filenames = files.map(p -> p.getFileName().toString()).toList();
            assertThat(filenames).contains("R__01_seed_demo_users.sql");
            assertThat(filenames).contains("R__02_seed_full_demo_users.sql");
            for (String name : filenames) {
                assertThat(name).startsWith("R__");
            }
        }
    }
}

