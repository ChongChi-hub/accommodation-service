package vn.toam.identity.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncoderTest {
    @Test
    void testBcryptHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String hash = encoder.encode("password123");
        System.out.println("BCRYPT_HASH_GENERATED: " + hash);
        assertTrue(encoder.matches("password123", hash));
    }
}

