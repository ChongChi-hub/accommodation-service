package vn.cozynest.identity.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import vn.cozynest.identity.domain.exception.IdentityDomainException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class GoogleTokenVerifier {

    private final String clientId;
    private final boolean enabled;
    private final String jwksUri;
    private JwtDecoder customDecoder;

    public GoogleTokenVerifier(
            @Value("${google.client-id:}") String clientId,
            @Value("${google.enabled:false}") boolean enabled,
            @Value("${google.jwks-uri:https://www.googleapis.com/oauth2/v3/certs}") String jwksUri) {
        this.clientId = clientId != null ? clientId.trim() : "";
        this.enabled = enabled;
        this.jwksUri = jwksUri;
    }

    public void setCustomDecoder(JwtDecoder customDecoder) {
        this.customDecoder = customDecoder;
    }

    public GoogleTokenClaims verify(String tokenString, String expectedNonce) {
        if (!enabled || clientId.isBlank()) {
            throw new IdentityDomainException("Tính năng đăng nhập Google chưa được cấu hình trên máy chủ", "ERR_GOOGLE_NOT_CONFIGURED");
        }

        JwtDecoder decoder = customDecoder;
        if (decoder == null) {
            try {
                decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
            } catch (Exception e) {
                throw new IdentityDomainException("Không thể khởi tạo cơ chế xác thực JWKS Google: " + e.getMessage(), "ERR_GOOGLE_NOT_CONFIGURED");
            }
        }

        Jwt jwt;
        try {
            jwt = decoder.decode(tokenString);
        } catch (Exception ex) {
            throw new IdentityDomainException("Chữ ký hoặc định dạng Google ID token không hợp lệ", "ERR_GOOGLE_TOKEN_INVALID");
        }

        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt == null || expiresAt.isBefore(Instant.now())) {
            throw new IdentityDomainException("Google ID token đã hết hạn", "ERR_GOOGLE_TOKEN_INVALID");
        }

        String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : "";
        if (!"https://accounts.google.com".equals(issuer) && !"accounts.google.com".equals(issuer)) {
            throw new IdentityDomainException("Issuer của Google ID token không hợp lệ: " + issuer, "ERR_GOOGLE_TOKEN_INVALID");
        }

        List<String> audience = jwt.getAudience();
        if (audience == null || !audience.contains(clientId)) {
            throw new IdentityDomainException("Audience của Google ID token không khớp với Google Client ID", "ERR_GOOGLE_TOKEN_INVALID");
        }

        Map<String, Object> claims = jwt.getClaims();
        Object emailVerifiedObj = claims.get("email_verified");
        boolean emailVerified = false;
        if (emailVerifiedObj instanceof Boolean b) {
            emailVerified = b;
        } else if (emailVerifiedObj instanceof String s) {
            emailVerified = Boolean.parseBoolean(s);
        }
        if (!emailVerified) {
            throw new IdentityDomainException("Email Google chưa được xác minh (email_verified=false)", "ERR_GOOGLE_EMAIL_NOT_VERIFIED");
        }

        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new IdentityDomainException("Google ID token thiếu thông tin định danh người dùng (sub)", "ERR_GOOGLE_TOKEN_INVALID");
        }

        String tokenNonce = jwt.getClaimAsString("nonce");
        if (tokenNonce == null || !tokenNonce.equals(expectedNonce)) {
            throw new IdentityDomainException("Mã xác thực nonce không khớp", "ERR_GOOGLE_NONCE_INVALID");
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new IdentityDomainException("Google ID token thiếu thông tin email", "ERR_GOOGLE_TOKEN_INVALID");
        }

        String name = jwt.getClaimAsString("name");
        if (name == null || name.isBlank()) {
            name = email.split("@")[0];
        }

        String picture = jwt.getClaimAsString("picture");
        String locale = jwt.getClaimAsString("locale");
        if (locale == null || locale.isBlank()) {
            locale = "vi";
        }

        return new GoogleTokenClaims(sub, email.trim().toLowerCase(), name, tokenNonce, picture, locale);
    }

    public record GoogleTokenClaims(String sub, String email, String name, String nonce, String pictureUrl, String locale) {
        public GoogleTokenClaims(String sub, String email, String name, String nonce) {
            this(sub, email, name, nonce, null, "vi");
        }
    }
}
