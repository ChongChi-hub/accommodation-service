package vn.cozynest.identity.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncoderTest {
    @Test
    void testBcryptHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String hash = encoder.encode("password123");
        System.out.println("BCRYPT_HASH_GENERATED: " + hash);
        boolean matches = encoder.matches("password123", "$2a$12$P9cFdZr5xw1vTc5Dee7Fy.o.lFyfDcfmTyRtBrsYqyFdGA7ig4D2a");
        System.out.println("SEED_HASH_MATCHES: " + matches);
        assertTrue(matches);
    }
}

