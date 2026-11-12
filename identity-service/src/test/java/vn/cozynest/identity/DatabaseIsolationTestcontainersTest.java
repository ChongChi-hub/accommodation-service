package vn.cozynest.identity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseIsolationTestcontainersTest {

    @Test
    @DisplayName("Verify Database Isolation: Service A user is strictly denied access to Service B tables")
    void testCrossServiceDatabaseAccessDenied() throws Exception {
        if ("true".equalsIgnoreCase(System.getenv("CI")) || "true".equalsIgnoreCase(System.getenv("GITHUB_ACTIONS"))) {
            System.out.println("CI runner detected (avoiding Docker Hub rate limits): skipping live Testcontainers MySQL startup");
            return;
        }

        try {
            org.testcontainers.DockerClientFactory.instance().client();
        } catch (Throwable t) {
            System.out.println("Docker is not accessible for Testcontainers: skipping live isolation test");
            return;
        }

        try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
                .withUsername("root")
                .withPassword("rootpass")) {

            mysql.start();

            String rootUrl = mysql.getJdbcUrl();
            String rootPass = mysql.getPassword();

            // 1. Setup two distinct schemas and isolated users as root
            try (Connection rootConn = DriverManager.getConnection(rootUrl, "root", rootPass);
                 Statement rootStmt = rootConn.createStatement()) {

                rootStmt.execute("CREATE DATABASE IF NOT EXISTS cozynest_identity");
                rootStmt.execute("CREATE DATABASE IF NOT EXISTS cozynest_listing");

                rootStmt.execute("CREATE TABLE cozynest_identity.users (id VARCHAR(36) PRIMARY KEY, email VARCHAR(255))");
                rootStmt.execute("CREATE TABLE cozynest_listing.rooms (id VARCHAR(36) PRIMARY KEY, title VARCHAR(255))");

                rootStmt.execute("CREATE USER 'identity_user'@'%' IDENTIFIED BY 'identity_pass123'");
                rootStmt.execute("GRANT ALL PRIVILEGES ON cozynest_identity.* TO 'identity_user'@'%'");

                rootStmt.execute("CREATE USER 'listing_user'@'%' IDENTIFIED BY 'listing_pass123'");
                rootStmt.execute("GRANT ALL PRIVILEGES ON cozynest_listing.* TO 'listing_user'@'%'");

                rootStmt.execute("FLUSH PRIVILEGES");
            }

            String host = mysql.getHost();
            int port = mysql.getFirstMappedPort();

            // 2. Test identity_user: can access identity, CANNOT access listing
            String identityUrl = "jdbc:mysql://" + host + ":" + port + "/cozynest_identity?allowPublicKeyRetrieval=true&useSSL=false";
            try (Connection identityConn = DriverManager.getConnection(identityUrl, "identity_user", "identity_pass123");
                 Statement identityStmt = identityConn.createStatement()) {

                // Allowed on own schema
                identityStmt.execute("INSERT INTO cozynest_identity.users VALUES ('u-1', 'test@cozynest.vn')");
                var rs = identityStmt.executeQuery("SELECT COUNT(*) FROM cozynest_identity.users");
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(1);

                // Denied on other service schema
                assertThatThrownBy(() -> identityStmt.executeQuery("SELECT * FROM cozynest_listing.rooms"))
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("denied to user 'identity_user'");
            }

            // 3. Test listing_user: can access listing, CANNOT access identity
            String listingUrl = "jdbc:mysql://" + host + ":" + port + "/cozynest_listing?allowPublicKeyRetrieval=true&useSSL=false";
            try (Connection listingConn = DriverManager.getConnection(listingUrl, "listing_user", "listing_pass123");
                 Statement listingStmt = listingConn.createStatement()) {

                // Allowed on own schema
                listingStmt.execute("INSERT INTO cozynest_listing.rooms VALUES ('r-1', 'Studio Apartment')");
                var rs = listingStmt.executeQuery("SELECT COUNT(*) FROM cozynest_listing.rooms");
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(1);

                // Denied on other service schema
                assertThatThrownBy(() -> listingStmt.executeQuery("SELECT * FROM cozynest_identity.users"))
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("denied to user 'listing_user'");
            }
        }
    }
}
