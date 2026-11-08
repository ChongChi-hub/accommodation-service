package vn.cozynest.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;

import java.security.Key;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JwksTokenValidator {

    private final Map<String, RSAPublicKey> keyRing = new ConcurrentHashMap<>();
    private final String expectedAudience;
    private final JwtParser parser;

    public JwksTokenValidator(Map<String, RSAPublicKey> initialKeys, String expectedAudience) {
        if (initialKeys != null) {
            this.keyRing.putAll(initialKeys);
        }
        this.expectedAudience = expectedAudience;

        Locator<Key> locator = new Locator<>() {
            @Override
            public Key locate(Header header) {
                if (header == null) {
                    throw new SecurityException("Missing JWT header");
                }
                Object algObj = header.get("alg");
                String alg = algObj != null ? String.valueOf(algObj) : null;
                if (alg == null || "none".equalsIgnoreCase(alg)) {
                    throw new SecurityException("Unsecured alg=none is forbidden");
                }

                Object kidObj = header.get("kid");
                String kid = kidObj != null ? String.valueOf(kidObj).trim() : null;
                if (kid == null || kid.isBlank()) {
                    throw new SecurityException("Missing kid in token header");
                }

                RSAPublicKey key = keyRing.get(kid);
                if (key == null) {
                    throw new SecurityException("Unknown kid: " + kid);
                }
                return key;
            }
        };

        var parserBuilder = Jwts.parser().keyLocator(locator);
        if (expectedAudience != null && !expectedAudience.isBlank()) {
            parserBuilder.requireAudience(expectedAudience);
        }
        this.parser = parserBuilder.build();
    }

    public void registerKey(String kid, RSAPublicKey key) {
        if (kid != null && key != null) {
            keyRing.put(kid, key);
        }
    }

    public void unregisterKey(String kid) {
        if (kid != null) {
            keyRing.remove(kid);
        }
    }

    public Claims validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new SecurityException("JWT token is empty");
        }
        try {
            return parser.parseSignedClaims(token.trim()).getPayload();
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            throw new SecurityException("Unsecured alg=none is forbidden: " + e.getMessage(), e);
        }
    }

    public Map<String, RSAPublicKey> getKeyRing() {
        return Map.copyOf(keyRing);
    }
}
