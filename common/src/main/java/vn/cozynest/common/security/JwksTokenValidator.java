package vn.cozynest.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Key;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class JwksTokenValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long REFRESH_COOLDOWN_MS = 5_000L;

    private final Map<String, RSAPublicKey> keyRing = new ConcurrentHashMap<>();
    private final String expectedAudience;
    private final String expectedIssuer;
    private final String jwksUri;
    private final HttpClient httpClient;
    private final JwtParser parser;
    private final AtomicLong lastRefreshMs = new AtomicLong(0L);

    public JwksTokenValidator(Map<String, RSAPublicKey> initialKeys, String expectedAudience) {
        this(initialKeys, expectedAudience, null, null);
    }

    public JwksTokenValidator(Map<String, RSAPublicKey> initialKeys,
                              String expectedAudience,
                              String expectedIssuer,
                              String jwksUri) {
        if (initialKeys != null) {
            this.keyRing.putAll(initialKeys);
        }
        this.expectedAudience = expectedAudience;
        this.expectedIssuer = expectedIssuer;
        this.jwksUri = (jwksUri != null && !jwksUri.isBlank()) ? jwksUri.trim() : null;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

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
                if (key == null && JwksTokenValidator.this.jwksUri != null) {
                    refreshFromJwks();
                    key = keyRing.get(kid);
                }
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
        if (expectedIssuer != null && !expectedIssuer.isBlank()) {
            parserBuilder.requireIssuer(expectedIssuer);
        }
        this.parser = parserBuilder.build();
    }

    public synchronized void refreshFromJwks() {
        if (jwksUri == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastRefreshMs.get() < REFRESH_COOLDOWN_MS && !keyRing.isEmpty()) {
            return;
        }
        lastRefreshMs.set(now);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(jwksUri))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body() == null) {
                return;
            }
            JsonNode root = MAPPER.readTree(response.body());
            JsonNode keysNode = root.get("keys");
            if (keysNode == null || !keysNode.isArray()) {
                return;
            }
            Map<String, RSAPublicKey> fetched = new HashMap<>();
            KeyFactory kf = KeyFactory.getInstance("RSA");
            for (JsonNode k : keysNode) {
                if (!"RSA".equalsIgnoreCase(k.path("kty").asText())) continue;
                String kid = k.path("kid").asText(null);
                String n = k.path("n").asText(null);
                String e = k.path("e").asText(null);
                if (kid == null || kid.isBlank() || n == null || e == null) continue;
                BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(n));
                BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));
                fetched.put(kid, (RSAPublicKey) kf.generatePublic(new RSAPublicKeySpec(modulus, exponent)));
            }
            if (!fetched.isEmpty()) {
                keyRing.putAll(fetched);
            }
        } catch (Exception ignored) {
            // Fail-closed: leave existing keyRing unchanged; unknown kids will be rejected
        }
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

    public static RSAPublicKey parsePublicKeyPem(String publicKeyPem) {
        try {
            String sanitized = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] pubBytes = Base64.getDecoder().decode(sanitized);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(pubBytes));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse RSA Public Key", e);
        }
    }

    public static List<String> extractRoleAuthorities(Claims claims) {
        String rolesStr = claims.get("roles", String.class);
        if (rolesStr == null || rolesStr.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rolesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .toList();
    }

    public Map<String, RSAPublicKey> getKeyRing() {
        return Map.copyOf(keyRing);
    }
}
