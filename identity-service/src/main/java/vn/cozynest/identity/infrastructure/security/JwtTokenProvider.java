package vn.cozynest.identity.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.cozynest.identity.application.port.TokenProviderPort;
import vn.cozynest.identity.domain.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider implements TokenProviderPort {

    public static final String ISSUER = "cozynest-identity";
    public static final String AUDIENCE = "cozynest-web";

    private final RsaKeyProvider rsaKeyProvider;
    private final long expirationMs;
    private vn.cozynest.identity.domain.repository.UserVerificationRepository userVerificationRepository;
    private vn.cozynest.identity.domain.repository.FaceVerificationRepository faceVerificationRepository;

    public JwtTokenProvider(RsaKeyProvider rsaKeyProvider) {
        this(rsaKeyProvider, 900000L);
    }

    @Autowired
    public JwtTokenProvider(
            RsaKeyProvider rsaKeyProvider,
            @Value("${jwt.expiration-ms:900000}") long expirationMs) {
        this.rsaKeyProvider = rsaKeyProvider;
        this.expirationMs = expirationMs;
    }

    @Autowired(required = false)
    public void setUserVerificationRepository(vn.cozynest.identity.domain.repository.UserVerificationRepository userVerificationRepository) {
        this.userVerificationRepository = userVerificationRepository;
    }

    @Autowired(required = false)
    public void setFaceVerificationRepository(vn.cozynest.identity.domain.repository.FaceVerificationRepository faceVerificationRepository) {
        this.faceVerificationRepository = faceVerificationRepository;
    }

    @Override
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        String rolesString = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));

        boolean emailVerified = user.getEmailVerifiedAt() != null;
        boolean kycVerified = userVerificationRepository != null && userVerificationRepository.findByUserId(user.getId())
                .map(v -> v.getStatus() == vn.cozynest.identity.domain.model.KycStatus.APPROVED).orElse(false);
        boolean faceVerified = faceVerificationRepository != null && faceVerificationRepository.findByUserId(user.getId())
                .map(f -> f.getStatus() == vn.cozynest.identity.domain.model.FaceVerificationStatus.PASSED).orElse(false);

        return Jwts.builder()
                .header().keyId(rsaKeyProvider.getKeyId()).and()
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("fullName", user.getFullName())
                .claim("roles", rolesString)
                .claim("email_verified", emailVerified)
                .claim("kyc_verified", kycVerified)
                .claim("face_verified", faceVerified)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(rsaKeyProvider.getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    @Override
    public String generateRefreshToken() {
        return UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
    }

    @Override
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    @Override
    public boolean validateToken(String token) {
        try {
            var untrusted = Jwts.parser().build().parseSignedClaims(token);
            String kid = untrusted.getHeader().getKeyId();
            var publicKey = rsaKeyProvider.getPublicKey(kid);

            Jwts.parser()
                    .requireIssuer(ISSUER)
                    .requireAudience(AUDIENCE)
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String extractUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(rsaKeyProvider.getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }
}
