package vn.cozynest.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DownstreamJwksSecurityMatrixTest {

    private static KeyPair keyPair1;
    private static KeyPair keyPair2;
    private static KeyPair rogueKeyPair;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        keyPair1 = gen.generateKeyPair();
        keyPair2 = gen.generateKeyPair();
        rogueKeyPair = gen.generateKeyPair();
    }

    private String createToken(RSAPrivateKey privateKey, String kid, String aud, Instant exp) {
        var builder = Jwts.builder()
                .subject("u-test-01")
                .claim("roles", "ROLE_TENANT")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(exp));

        if (kid != null) {
            builder.header().keyId(kid);
        }
        if (aud != null) {
            builder.audience().add(aud);
        }
        return builder.signWith(privateKey, Jwts.SIG.RS256).compact();
    }

    @Test
    @DisplayName("1. Valid token with kid, audience, and active expiry succeeds")
    void testValidToken() {
        JwksTokenValidator validator = new JwksTokenValidator(
                Map.of("kid-1", (RSAPublicKey) keyPair1.getPublic()),
                "cozynest-api"
        );
        String token = createToken((RSAPrivateKey) keyPair1.getPrivate(), "kid-1", "cozynest-api", Instant.now().plus(15, ChronoUnit.MINUTES));
        Claims claims = validator.validateToken(token);
        assertThat(claims.getSubject()).isEqualTo("u-test-01");
        assertThat(claims.get("roles")).isEqualTo("ROLE_TENANT");
    }

    @Test
    @DisplayName("2. Expired token is rejected with 401 (ExpiredJwtException)")
    void testExpiredToken() {
        JwksTokenValidator validator = new JwksTokenValidator(
                Map.of("kid-1", (RSAPublicKey) keyPair1.getPublic()),
                "cozynest-api"
        );
        String expiredToken = createToken((RSAPrivateKey) keyPair1.getPrivate(), "kid-1", "cozynest-api", Instant.now().minus(5, ChronoUnit.MINUTES));
        assertThatThrownBy(() -> validator.validateToken(expiredToken))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    @DisplayName("3. Token signed with wrong key is rejected with 401 (SignatureException)")
    void testWrongSignature() {
        JwksTokenValidator validator = new JwksTokenValidator(
                Map.of("kid-1", (RSAPublicKey) keyPair1.getPublic()),
                "cozynest-api"
        );
        String forgedToken = createToken((RSAPrivateKey) rogueKeyPair.getPrivate(), "kid-1", "cozynest-api", Instant.now().plus(15, ChronoUnit.MINUTES));
        assertThatThrownBy(() -> validator.validateToken(forgedToken))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }

    @Test
    @DisplayName("4. Token with alg=none is rejected with 401 (SecurityException)")
    void testAlgNoneToken() {
        JwksTokenValidator validator = new JwksTokenValidator(
                Map.of("kid-1", (RSAPublicKey) keyPair1.getPublic()),
                "cozynest-api"
        );
        // Header {"alg":"none","kid":"kid-1"} + payload {"sub":"u-test-01"} + empty signature
        String header = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\",\"kid\":\"kid-1\"}".getBytes());
        String payload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"u-test-01\",\"aud\":[\"cozynest-api\"],\"exp\":9999999999}".getBytes());
        String algNoneToken = header + "." + payload + ".";

        assertThatThrownBy(() -> validator.validateToken(algNoneToken))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("alg=none");
    }

    @Test
    @DisplayName("5. Token missing expected audience is rejected with 401 (MissingClaimException)")
    void testMissingAudience() {
        JwksTokenValidator validator = new JwksTokenValidator(
                Map.of("kid-1", (RSAPublicKey) keyPair1.getPublic()),
                "cozynest-api"
        );
        String tokenNoAud = createToken((RSAPrivateKey) keyPair1.getPrivate(), "kid-1", null, Instant.now().plus(15, ChronoUnit.MINUTES));
        assertThatThrownBy(() -> validator.validateToken(tokenNoAud))
                .isInstanceOf(io.jsonwebtoken.MissingClaimException.class);
    }

    @Test
    @DisplayName("6. Token missing kid header is rejected with 401 (SecurityException)")
    void testMissingKidHeader() {
        JwksTokenValidator validator = new JwksTokenValidator(
                Map.of("kid-1", (RSAPublicKey) keyPair1.getPublic()),
                "cozynest-api"
        );
        String tokenNoKid = createToken((RSAPrivateKey) keyPair1.getPrivate(), null, "cozynest-api", Instant.now().plus(15, ChronoUnit.MINUTES));
        assertThatThrownBy(() -> validator.validateToken(tokenNoKid))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Missing kid");
    }

    @Test
    @DisplayName("7. Key Rotation: Downstream supports dual active kids simultaneously")
    void testDualKidRotation() {
        JwksTokenValidator validator = new JwksTokenValidator(
                Map.of(
                        "kid-1", (RSAPublicKey) keyPair1.getPublic(),
                        "kid-2", (RSAPublicKey) keyPair2.getPublic()
                ),
                "cozynest-api"
        );

        String token1 = createToken((RSAPrivateKey) keyPair1.getPrivate(), "kid-1", "cozynest-api", Instant.now().plus(15, ChronoUnit.MINUTES));
        String token2 = createToken((RSAPrivateKey) keyPair2.getPrivate(), "kid-2", "cozynest-api", Instant.now().plus(15, ChronoUnit.MINUTES));

        Claims claims1 = validator.validateToken(token1);
        Claims claims2 = validator.validateToken(token2);

        assertThat(claims1.getSubject()).isEqualTo("u-test-01");
        assertThat(claims2.getSubject()).isEqualTo("u-test-01");
    }
}

