package vn.cozynest.common.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DownstreamJwtVerifierTest {

    @Test
    void productionProfileRejectsBlankPublicKey() {
        MockEnvironment prod = new MockEnvironment().withProperty("dummy", "1");
        prod.setActiveProfiles("prod");
        assertThatThrownBy(() -> new DownstreamJwtVerifier("", prod))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.public-key");
    }

    @Test
    void verifiesValidTokenAndSupportsKeyRotation() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();
        String pubBase64 = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());

        DownstreamJwtVerifier verifier = new DownstreamJwtVerifier("", new MockEnvironment());
        verifier.registerRotatedKey("rotated-key-2", pubBase64);

        String token = Jwts.builder()
                .header().keyId("rotated-key-2").and()
                .subject("u-admin-1")
                .audience().add(DownstreamJwtVerifier.EXPECTED_AUDIENCE).and()
                .claim("roles", "ADMIN,LANDLORD")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith((RSAPrivateKey) kp.getPrivate(), Jwts.SIG.RS256)
                .compact();

        var principal = verifier.verifyToken(token);
        assertThat(principal.userId()).isEqualTo("u-admin-1");
        assertThat(principal.roles()).containsExactly("ROLE_ADMIN", "ROLE_LANDLORD");
    }

    @Test
    void rejectsWrongAudienceAndUnknownKid() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();
        String pubBase64 = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
        DownstreamJwtVerifier verifier = new DownstreamJwtVerifier(pubBase64, new MockEnvironment());

        String wrongAud = Jwts.builder()
                .header().keyId(DownstreamJwtVerifier.DEFAULT_KEY_ID).and()
                .subject("u-1")
                .audience().add("wrong-aud").and()
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith((RSAPrivateKey) kp.getPrivate(), Jwts.SIG.RS256)
                .compact();

        assertThatThrownBy(() -> verifier.verifyToken(wrongAud))
                .isInstanceOf(RuntimeException.class);
    }
}

