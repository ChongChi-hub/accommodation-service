package vn.cozynest.common.security;

import io.jsonwebtoken.Claims;
import org.springframework.core.env.Environment;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Shared downstream JWT verifier wrapping {@link JwksTokenValidator}.
 * Enforces kid pinning, audience check, and rejects alg=none or missing keys in prod.
 */
public class DownstreamJwtVerifier {

    public static final String DEFAULT_KEY_ID = "cozynest-identity-key-1";
    public static final String EXPECTED_AUDIENCE = "cozynest-web";
    private static final String DEV_FALLBACK_PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA56BlqpTtCQBNqpLJwhL50U6pMMxUukLlckNBBfnci7TAmYDY9Bs/f9LEAGUY3wCch+cN+dz+ofo3E/AO+AX7xEbyVw+4z9m3K5GJgUQ2jmIMUBkAuwPiCTmW/HaTccp4Sbh5ou7WYLVaQuswIwCKa1Wo8giycy0UnLWBzvKickT+9FGjHz4fQDScFnGxp1tWhqfAzJUGAh7F1WT3qRtb+HG6rZxMCjNHRGlbvyxTOWMF/0zap+CrDiHwluCWYmHJ5Rta8nxCDx8biNLiThvLp4Dh6rkmw2Kfv/ZnCDXifGVMVyZlrKwVNFvFTq13DiCYV81MXUMJoL6ZiKZ9rkHbgwIDAQAB";

    private final JwksTokenValidator validator;

    public DownstreamJwtVerifier(String publicKeyPem, Environment env) {
        this(publicKeyPem, EXPECTED_AUDIENCE, env);
    }

    public DownstreamJwtVerifier(String publicKeyPem, String expectedAudience, Environment env) {
        this(publicKeyPem, null, null, expectedAudience, DEFAULT_KEY_ID, env);
    }

    public DownstreamJwtVerifier(String publicKeyPem,
                                 String jwksUri,
                                 String expectedIssuer,
                                 String expectedAudience,
                                 String defaultKeyId,
                                 Environment env) {
        boolean isProd = env != null && env.matchesProfiles("prod", "production");
        boolean hasPublicKey = publicKeyPem != null && !publicKeyPem.isBlank();
        boolean hasJwksUri = jwksUri != null && !jwksUri.isBlank();
        if (isProd && !hasPublicKey && !hasJwksUri) {
            throw new IllegalStateException("Production profile requires explicit jwt.public-key or jwt.jwks-uri configuration.");
        }
        String keyId = (defaultKeyId != null && !defaultKeyId.isBlank()) ? defaultKeyId : DEFAULT_KEY_ID;
        Map<String, RSAPublicKey> initialKeys;
        if (hasPublicKey) {
            initialKeys = Map.of(keyId, parsePublicKey(publicKeyPem));
        } else if (!hasJwksUri && !isProd) {
            initialKeys = Map.of(DEFAULT_KEY_ID, parsePublicKey(DEV_FALLBACK_PUBLIC_KEY));
        } else {
            initialKeys = Map.of();
        }
        this.validator = new JwksTokenValidator(initialKeys, expectedAudience, expectedIssuer, jwksUri);
    }

    private DownstreamJwtVerifier(JwksTokenValidator validator) {
        this.validator = validator;
    }

    public static DownstreamJwtVerifier forTest(RSAPublicKey publicKey, String keyId) {
        return new DownstreamJwtVerifier(new JwksTokenValidator(
                Map.of(keyId, publicKey), EXPECTED_AUDIENCE, "cozynest-identity", null));
    }

    public VerifiedPrincipal verifyToken(String token) {
        Claims claims = validator.validateToken(token);
        String userId = claims.getSubject();
        String rolesStr = claims.get("roles", String.class);
        List<String> roles = List.of();
        if (rolesStr != null && !rolesStr.isBlank()) {
            roles = Arrays.stream(rolesStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .toList();
        }
        return new VerifiedPrincipal(userId, roles, claims);
    }

    public void registerRotatedKey(String kid, String publicKeyPem) {
        validator.registerKey(kid, parsePublicKey(publicKeyPem));
    }

    public static RSAPublicKey parsePublicKey(String publicKeyPem) {
        try {
            String base64 = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] pubBytes = Base64.getDecoder().decode(base64);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(pubBytes));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize RSA Public Key for JWT validation", e);
        }
    }

    public record VerifiedPrincipal(String userId, List<String> roles, Claims claims) {}
}

