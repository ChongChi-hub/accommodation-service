package vn.cozynest.identity.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final RsaKeyProvider rsaKeyProvider;

    public JwtAuthFilter(RsaKeyProvider rsaKeyProvider) {
        this.rsaKeyProvider = rsaKeyProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // Parse header to get kid
                var untrusted = Jwts.parser().build().parseSignedClaims(token);
                String kid = untrusted.getHeader().getKeyId();
                var publicKey = rsaKeyProvider.getPublicKey(kid);

                Claims claims = Jwts.parser()
                        .requireIssuer(JwtTokenProvider.ISSUER)
                        .requireAudience(JwtTokenProvider.AUDIENCE)
                        .verifyWith(publicKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String userId = claims.getSubject();
                String rolesStr = claims.get("roles", String.class);

                Set<SimpleGrantedAuthority> authorities = new HashSet<>();
                if (rolesStr != null && !rolesStr.isBlank()) {
                    Arrays.stream(rolesStr.split(",")).map(String::trim).forEach(role -> {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                        mapRoleToPermissions(role, authorities);
                    });
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private void mapRoleToPermissions(String role, Set<SimpleGrantedAuthority> authorities) {
        switch (role) {
            case "ADMIN" -> {
                authorities.add(new SimpleGrantedAuthority("admin:all"));
                authorities.add(new SimpleGrantedAuthority("user:manage"));
                authorities.add(new SimpleGrantedAuthority("kyc:review"));
                authorities.add(new SimpleGrantedAuthority("room:moderate"));
            }
            case "MODERATOR" -> {
                authorities.add(new SimpleGrantedAuthority("kyc:review"));
                authorities.add(new SimpleGrantedAuthority("room:moderate"));
            }
            case "LANDLORD" -> {
                authorities.add(new SimpleGrantedAuthority("room:create"));
                authorities.add(new SimpleGrantedAuthority("room:manage"));
                authorities.add(new SimpleGrantedAuthority("appointment:manage"));
            }
            case "TENANT" -> {
                authorities.add(new SimpleGrantedAuthority("appointment:request"));
                authorities.add(new SimpleGrantedAuthority("rental:request"));
            }
        }
    }
}
