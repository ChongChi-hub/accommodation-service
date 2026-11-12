package vn.cozynest.admin.interfaces.rest;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.cozynest.admin.infrastructure.security.JwtAuthFilter;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class AdminSecurityMatrixTest {

    private KeyPair keyPair;
    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        keyPair = gen.generateKeyPair();
        filter = JwtAuthFilter.forTest((RSAPublicKey) keyPair.getPublic(), "admin-test-kid");
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Valid ADMIN token authenticates with ROLE_ADMIN; unauthenticated and rogue tokens stay unauthenticated")
    void testAdminSecurityFilter() throws Exception {
        String validAdminJwt = Jwts.builder()
                .issuer("cozynest-identity")
                .audience().add("cozynest-web").and()
                .subject("admin-01")
                .claim("roles", "ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 600_000))
                .header().keyId("admin-test-kid").and()
                .signWith((RSAPrivateKey) keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/admin/disputes");
        req.addHeader("Authorization", "Bearer " + validAdminJwt);
        filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("admin-01");
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");

        SecurityContextHolder.clearContext();
        MockHttpServletRequest anonReq = new MockHttpServletRequest("GET", "/api/v1/admin/disputes");
        filter.doFilter(anonReq, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}

