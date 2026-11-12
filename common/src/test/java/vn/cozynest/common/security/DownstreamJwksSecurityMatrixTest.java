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

    @Test
    @DisplayName("8. Wrong issuer is rejected and live HTTP JWKS rotation fetches new kid dynamically")
    void testDynamicHttpJwksRotationAndIssuerCheck() throws Exception {
        RSAPublicKey pub2 = (RSAPublicKey) keyPair2.getPublic();
        String n = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(unsignedBytes(pub2.getModulus()));
        String e = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(unsignedBytes(pub2.getPublicExponent()));
        String jwksJson = "{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"alg\":\"RS256\",\"kid\":\"kid-rot-2\",\"n\":\"" + n + "\",\"e\":\"" + e + "\"}]}";

        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(0), 0);
        server.createContext("/.well-known/jwks.json", ex -> {
            byte[] body = jwksJson.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, body.length);
            try (var os = ex.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        try {
            String uri = "http://localhost:" + server.getAddress().getPort() + "/.well-known/jwks.json";
            JwksTokenValidator validator = new JwksTokenValidator(Map.of(), "cozynest-web", "cozynest-identity", uri);

            String validRotated = Jwts.builder()
                    .issuer("cozynest-identity")
                    .audience().add("cozynest-web").and()
                    .subject("u-rot-01")
                    .claim("roles", "LANDLORD")
                    .issuedAt(new Date())
                    .expiration(Date.from(Instant.now().plus(10, ChronoUnit.MINUTES)))
                    .header().keyId("kid-rot-2").and()
                    .signWith((RSAPrivateKey) keyPair2.getPrivate(), Jwts.SIG.RS256)
                    .compact();

            Claims claims = validator.validateToken(validRotated);
            assertThat(claims.getSubject()).isEqualTo("u-rot-01");
            assertThat(JwksTokenValidator.extractRoleAuthorities(claims)).containsExactly("ROLE_LANDLORD");

            String wrongIssuer = Jwts.builder()
                    .issuer("evil-issuer")
                    .audience().add("cozynest-web").and()
                    .subject("u-rot-01")
                    .issuedAt(new Date())
                    .expiration(Date.from(Instant.now().plus(10, ChronoUnit.MINUTES)))
                    .header().keyId("kid-rot-2").and()
                    .signWith((RSAPrivateKey) keyPair2.getPrivate(), Jwts.SIG.RS256)
                    .compact();
            assertThatThrownBy(() -> validator.validateToken(wrongIssuer))
                    .isInstanceOf(io.jsonwebtoken.IncorrectClaimException.class);
        } finally {
            server.stop(0);
        }
    }

    private static byte[] unsignedBytes(java.math.BigInteger bi) {
        byte[] bytes = bi.toByteArray();
        if (bytes.length > 0 && bytes[0] == 0) {
            byte[] tmp = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, tmp, 0, tmp.length);
            return tmp;
        }
        return bytes;
    }
}

