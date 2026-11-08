package vn.cozynest.booking.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final RSAPublicKey publicKey;

    public JwtAuthFilter(@Value("${jwt.public-key:}") String publicKeyPem) {
        try {
            String pubBase64 = (publicKeyPem != null && !publicKeyPem.isBlank())
                    ? publicKeyPem
                    : "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA56BlqpTtCQBNqpLJwhL50U6pMMxUukLlckNBBfnci7TAmYDY9Bs/f9LEAGUY3wCch+cN+dz+ofo3E/AO+AX7xEbyVw+4z9m3K5GJgUQ2jmIMUBkAuwPiCTmW/HaTccp4Sbh5ou7WYLVaQuswIwCKa1Wo8giycy0UnLWBzvKickT+9FGjHz4fQDScFnGxp1tWhqfAzJUGAh7F1WT3qRtb+HG6rZxMCjNHRGlbvyxTOWMF/0zap+CrDiHwluCWYmHJ5Rta8nxCDx8biNLiThvLp4Dh6rkmw2Kfv/ZnCDXifGVMVyZlrKwVNFvFTq13DiCYV81MXUMJoL6ZiKZ9rkHbgwIDAQAB";

            pubBase64 = pubBase64.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] pubBytes = Base64.getDecoder().decode(pubBase64);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            this.publicKey = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(pubBytes));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize RSA Public Key for JWT validation", e);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(publicKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String userId = claims.getSubject();
                String rolesStr = claims.get("roles", String.class);

                List<SimpleGrantedAuthority> authorities = List.of();
                if (rolesStr != null && !rolesStr.isBlank()) {
                    authorities = Arrays.stream(rolesStr.split(","))
                            .map(role -> {
                                String r = role.trim();
                                return r.startsWith("ROLE_") ? new SimpleGrantedAuthority(r) : new SimpleGrantedAuthority("ROLE_" + r);
                            })
                            .toList();
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
