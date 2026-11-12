package vn.cozynest.billing.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.cozynest.common.security.DownstreamJwtVerifier;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final DownstreamJwtVerifier verifier;

    public JwtAuthFilter(String publicKeyPem) {
        this.verifier = new DownstreamJwtVerifier(publicKeyPem, null);
    }

    @Autowired
    public JwtAuthFilter(
            @Value("${jwt.jwks-uri:${IDENTITY_SERVICE_URL:http://localhost:8081}/.well-known/jwks.json}") String jwksUri,
            @Value("${jwt.public-key:}") String publicKeyPem,
            @Value("${jwt.issuer:cozynest-identity}") String expectedIssuer,
            @Value("${jwt.audience:cozynest-web}") String expectedAudience,
            @Value("${jwt.key-id:cozynest-identity-key-1}") String defaultKeyId,
            @Autowired(required = false) Environment env) {
        this.verifier = new DownstreamJwtVerifier(
                publicKeyPem, jwksUri, expectedIssuer, expectedAudience, defaultKeyId, env);
    }

    private JwtAuthFilter(DownstreamJwtVerifier verifier) {
        this.verifier = verifier;
    }

    public static JwtAuthFilter forTest(RSAPublicKey publicKey, String keyId) {
        return new JwtAuthFilter(DownstreamJwtVerifier.forTest(publicKey, keyId));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                DownstreamJwtVerifier.VerifiedPrincipal principal = verifier.verifyToken(authHeader.substring(7));
                List<SimpleGrantedAuthority> authorities = principal.roles().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal.userId(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
