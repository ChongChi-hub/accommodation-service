package vn.cozynest.identity.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import vn.cozynest.identity.application.port.PasswordEncoderPort;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncoderTest {
    @Test
    void testBcryptHashAndCost12() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String hash = encoder.encode("password123");
        assertTrue(hash.startsWith("$2a$12$") || hash.startsWith("$2b$12$"));
        assertTrue(encoder.matches("password123", hash));
    }

    @Test
    void testUpgradeEncodingOnOldCost() {
        BCryptPasswordEncoder encoder12 = new BCryptPasswordEncoder(12);
        BCryptPasswordEncoder encoder10 = new BCryptPasswordEncoder(10);
        PasswordEncoderPort port = new PasswordEncoderConfig.PasswordEncoderAdapter(encoder12);

        String oldHash = encoder10.encode("password123");
        String newHash = encoder12.encode("password123");

        assertTrue(port.upgradeEncoding(oldHash));
        assertFalse(port.upgradeEncoding(newHash));
    }
}

