package vn.cozynest.identity.infrastructure.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import vn.cozynest.identity.domain.exception.IdentityDomainException;

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

class GoogleTokenVerifierTest {

    private static final String TEST_CLIENT_ID = "test-google-client-id.apps.googleusercontent.com";
    private KeyPair keyPair;
    private KeyPair otherKeyPair;
    private GoogleTokenVerifier verifier;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        keyPair = kpg.generateKeyPair();
        otherKeyPair = kpg.generateKeyPair();

        verifier = new GoogleTokenVerifier(TEST_CLIENT_ID, true, "https://www.googleapis.com/oauth2/v3/certs");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
        verifier.setCustomDecoder(decoder);
    }

    private String buildToken(String iss, String aud, String sub, String email, boolean emailVerified,
                              String nonce, Instant exp, RSAPrivateKey privateKey) {
        return Jwts.builder()
                .issuer(iss)
                .audience().add(aud).and()
                .subject(sub)
                .claim("email", email)
                .claim("email_verified", emailVerified)
                .claim("name", "Nguyen Van Test")
                .claim("nonce", nonce)
                .expiration(Date.from(exp))
                .signWith(privateKey)
                .compact();
    }

    @Test
    @DisplayName("Hợp lệ: Token đúng mọi tiêu chuẩn -> xác minh thành công")
    void testValidToken() {
        String token = buildToken(
                "https://accounts.google.com",
                TEST_CLIENT_ID,
                "google-sub-12345",
                "test@gmail.com",
                true,
                "nonce-valid-xyz",
                Instant.now().plus(1, ChronoUnit.HOURS),
                (RSAPrivateKey) keyPair.getPrivate()
        );

        var claims = verifier.verify(token, "nonce-valid-xyz");
        assertThat(claims.sub()).isEqualTo("google-sub-12345");
        assertThat(claims.email()).isEqualTo("test@gmail.com");
        assertThat(claims.name()).isEqualTo("Nguyen Van Test");
        assertThat(claims.nonce()).isEqualTo("nonce-valid-xyz");
    }

    @Test
    @DisplayName("Sai aud: Token có audience không khớp Client ID -> ném ERR_GOOGLE_TOKEN_INVALID")
    void testInvalidAudience() {
        String token = buildToken(
                "https://accounts.google.com",
                "wrong-client-id.apps.googleusercontent.com",
                "google-sub-12345",
                "test@gmail.com",
                true,
                "nonce-xyz",
                Instant.now().plus(1, ChronoUnit.HOURS),
                (RSAPrivateKey) keyPair.getPrivate()
        );

        assertThatThrownBy(() -> verifier.verify(token, "nonce-xyz"))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_GOOGLE_TOKEN_INVALID");
    }

    @Test
    @DisplayName("Sai iss: Token có issuer lạ không thuộc accounts.google.com -> ném ERR_GOOGLE_TOKEN_INVALID")
    void testInvalidIssuer() {
        String token = buildToken(
                "https://evil.com",
                TEST_CLIENT_ID,
                "google-sub-12345",
                "test@gmail.com",
                true,
                "nonce-xyz",
                Instant.now().plus(1, ChronoUnit.HOURS),
                (RSAPrivateKey) keyPair.getPrivate()
        );

        assertThatThrownBy(() -> verifier.verify(token, "nonce-xyz"))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_GOOGLE_TOKEN_INVALID");
    }

    @Test
    @DisplayName("Hết hạn: Token quá hạn exp -> ném ERR_GOOGLE_TOKEN_INVALID")
    void testExpiredToken() {
        String token = buildToken(
                "https://accounts.google.com",
                TEST_CLIENT_ID,
                "google-sub-12345",
                "test@gmail.com",
                true,
                "nonce-xyz",
                Instant.now().minus(5, ChronoUnit.MINUTES),
                (RSAPrivateKey) keyPair.getPrivate()
        );

        assertThatThrownBy(() -> verifier.verify(token, "nonce-xyz"))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_GOOGLE_TOKEN_INVALID");
    }

    @Test
    @DisplayName("Sai chữ ký: Token ký bằng khóa lạ khác JWKS -> ném ERR_GOOGLE_TOKEN_INVALID")
    void testInvalidSignature() {
        String token = buildToken(
                "https://accounts.google.com",
                TEST_CLIENT_ID,
                "google-sub-12345",
                "test@gmail.com",
                true,
                "nonce-xyz",
                Instant.now().plus(1, ChronoUnit.HOURS),
                (RSAPrivateKey) otherKeyPair.getPrivate()
        );

        assertThatThrownBy(() -> verifier.verify(token, "nonce-xyz"))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_GOOGLE_TOKEN_INVALID");
    }

    @Test
    @DisplayName("email_verified=false: Email chưa xác thực phía Google -> ném ERR_GOOGLE_EMAIL_NOT_VERIFIED")
    void testEmailNotVerified() {
        String token = buildToken(
                "https://accounts.google.com",
                TEST_CLIENT_ID,
                "google-sub-12345",
                "test@gmail.com",
                false,
                "nonce-xyz",
                Instant.now().plus(1, ChronoUnit.HOURS),
                (RSAPrivateKey) keyPair.getPrivate()
        );

        assertThatThrownBy(() -> verifier.verify(token, "nonce-xyz"))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_GOOGLE_EMAIL_NOT_VERIFIED");
    }

    @Test
    @DisplayName("Thiếu sub: Token không có claim sub -> ném ERR_GOOGLE_TOKEN_INVALID")
    void testMissingSub() {
        String token = Jwts.builder()
                .issuer("https://accounts.google.com")
                .audience().add(TEST_CLIENT_ID).and()
                .claim("email", "test@gmail.com")
                .claim("email_verified", true)
                .claim("nonce", "nonce-xyz")
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith((RSAPrivateKey) keyPair.getPrivate())
                .compact();

        assertThatThrownBy(() -> verifier.verify(token, "nonce-xyz"))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_GOOGLE_TOKEN_INVALID");
    }

    @Test
    @DisplayName("Sai nonce: Nonce trong token không khớp nonce gửi lên -> ném ERR_GOOGLE_NONCE_INVALID")
    void testMismatchedNonce() {
        String token = buildToken(
                "https://accounts.google.com",
                TEST_CLIENT_ID,
                "google-sub-12345",
                "test@gmail.com",
                true,
                "nonce-expected-abc",
                Instant.now().plus(1, ChronoUnit.HOURS),
                (RSAPrivateKey) keyPair.getPrivate()
        );

        assertThatThrownBy(() -> verifier.verify(token, "different-nonce-xyz"))
                .isInstanceOf(IdentityDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_GOOGLE_NONCE_INVALID");
    }
}

