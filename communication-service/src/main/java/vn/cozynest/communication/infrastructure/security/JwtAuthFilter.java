package vn.cozynest.communication.infrastructure.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.cozynest.common.security.JwksTokenValidator;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwksTokenValidator validator;

    @Autowired
    public JwtAuthFilter(
            @Value("${jwt.jwks-uri:${IDENTITY_SERVICE_URL:http://localhost:8081}/.well-known/jwks.json}") String jwksUri,
            @Value("${jwt.public-key:}") String publicKeyPem,
            @Value("${jwt.issuer:cozynest-identity}") String expectedIssuer,
            @Value("${jwt.audience:cozynest-web}") String expectedAudience,
            @Value("${jwt.key-id:cozynest-identity-key-1}") String defaultKeyId) {
        Map<String, RSAPublicKey> initialKeys = (publicKeyPem != null && !publicKeyPem.isBlank())
                ? Map.of(defaultKeyId, JwksTokenValidator.parsePublicKeyPem(publicKeyPem))
                : Map.of();
        this.validator = new JwksTokenValidator(initialKeys, expectedAudience, expectedIssuer, jwksUri);
    }

    private JwtAuthFilter(JwksTokenValidator validator) {
        this.validator = validator;
    }

    public static JwtAuthFilter forTest(RSAPublicKey publicKey, String keyId) {
        return new JwtAuthFilter(new JwksTokenValidator(Map.of(keyId, publicKey), "cozynest-web", "cozynest-identity", null));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                Claims claims = validator.validateToken(authHeader.substring(7));
                List<SimpleGrantedAuthority> authorities = JwksTokenValidator.extractRoleAuthorities(claims).stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
