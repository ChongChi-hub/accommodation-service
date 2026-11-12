package vn.cozynest.booking;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.cozynest.booking.infrastructure.security.JwtAuthFilter;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class LandlordAppointmentSecurityMatrixTest {

    private JwtAuthFilter jwtAuthFilter;
    private RSAPrivateKey privateKey;

    private static final String PRIVATE_KEY_PEM =
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDnoGWqlO0JAE2qksnCEvnRTqkwzFS6QuVyQ0EF+dyLtMCZgNj0Gz9/0sQAZRjfAJyH5w353P6h+jcT8A74BfvERvJXD7jP2bcrkYmBRDaOYgxQGQC7A+IJOZb8dpNxynhJuHmi7tZgtVpC6zAjAIprVajyCLJzLRSctYHO8qJyRP70UaMfPh9ANJwWcbGnW1aGp8DMlQYCHsXVZPepG1v4cbqtnEwKM0dEaVu/LFM5YwX/TNqn4KsOIfCW4JZiYcnlG1ryfEIPHxuI0uJOG8ungOHquSbDYp+/9mcINeJ8ZUxXJmWsrBU0W8VOrXcOIJhXzUxdQwmgvpmIpn2uQduDAgMBAAECggEAIAq2RbS4XahnFhOcSXaqjGPfT9rUVbfvWbEoC5WXBtSIWDPUz1/9lscnSg6yNBc3kC1MZ6ZEq0+b0uxAhdr+Cxbta1HHdSlfB23oobM4ANZbcbEjwrs0x6Xp1x7oIrOplDkqynlTuqkbxWoXjKAEhfihmGnPldNEqLELNf+mrTvOwiR3vLKNPkIbhoPWU0Or7t0XmkwZ9ku02EfisqT+nAgZufOiLU+AXEcrfEcVK0TuMKT7B4UcQw8tjDQtgBKsC4+/vxgPk1O3aMru720fSx8MntToLvAkq2nFZ/t5okROij7VBnIQeGXp84Bdmpzu7Ac+ji9Ls4omW0zQ25S5QQKBgQD+JW0QH8if+lXX2M7/ECiylCyLcze+lv3LYp3tqV3wrwUS4vACZJWau7PmHM/wyauXDrTyUn90pFoSkTRj3kr4QyMc6dGdM0fgFmp+ztrDlqM6XRqaVpsW2hhm+2umDtaonBCrYRXlvTDePi6ekbniZ+oQDNt8iMz2nWj78HXMkQKBgQDpUOtpOusxM7QiXdgZV/I5s/3Dy613Pcn5mgG+wnk3+pDOXMqN9GhCFdo7Ms6mBtcTEUJbhyFKD8abkZ/zXsEks1AxaorxV8Qt72Bu84iteTiy9ubOuwtQxkkNnGFfkAIqAxufJrv/ZHaujLshJCwo1gR7lPgSvuMuij1yJKZA0wKBgFYh0wOq8fCeA+fiV80D6fcT1sDwLI1QWPVMF+ALDYZbmqma9eKpujY1tTOgwiyF/Lx2RIb3xLCAXvJ5+bp7BQd8oXLUH7FaRtrAXhlZ+7Rb4jgOg8yY23j0M177iLE/yCpeshuTDLvO0bgQdPZSxx/fPelwohZjuqs+D+2KwjGxAoGBAIYGnOSbGuXtrImdXPjaA2RQV/L0epYn4XFHnb8GGfolLq/Mr/Kxtn5X7T/aZMkM3tmuLW9EhcP0ogNVuQtloA/EDcoFSQFwiFpWyw+RSC9WllJ/8IBLBCoHbYmrDmiF4iheFqzq5E3GWULVxt032UIwBu0y2vpxuzFN6hQ6p2JPAoGAbLRSNpK6wOY88iCSCH8pFv3hRbfEQUWlcNbQ/1FmueiQaCr4ucq9LCat4W3LjKth9kqVvlL6pqEp04U1FD0YqDZpT0CVWG0eWqgHz95gDc4HwnuaURrsyjBrI9GB/m3zyGbggsbVaWDFFgOQv6VPCN0wZrPpRg+zdvOuTy/qbco=";

    @BeforeEach
    void setUp() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        byte[] privBytes = Base64.getDecoder().decode(PRIVATE_KEY_PEM.replaceAll("\\s+", ""));
        this.privateKey = (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
        var crt = (java.security.interfaces.RSAPrivateCrtKey) this.privateKey;
        var pub = (java.security.interfaces.RSAPublicKey) kf.generatePublic(
                new java.security.spec.RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent()));
        jwtAuthFilter = JwtAuthFilter.forTest(pub, "cozynest-identity-key-1");
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String generateToken(String userId, List<String> roles) {
        return Jwts.builder()
                .issuer("cozynest-identity")
                .audience().add("cozynest-web").and()
                .subject(userId)
                .claim("roles", String.join(",", roles))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .header().keyId("cozynest-identity-key-1").and()
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    @Test
    @DisplayName("GUEST: Request without token has no authentication")
    void testGuestHasNoAuth() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/landlord/appointments");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthFilter.doFilter(req, res, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("LANDLORD: Request with ROLE_LANDLORD gets authenticated with ROLE_LANDLORD authority")
    void testLandlordHasRole() throws Exception {
        String token = generateToken("u-landlord-01", List.of("ROLE_LANDLORD"));
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/landlord/appointments");
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthFilter.doFilter(req, res, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("u-landlord-01", auth.getPrincipal());
        assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_LANDLORD"));
    }

    @Test
    @DisplayName("TENANT: Request with ROLE_TENANT does NOT have ROLE_LANDLORD")
    void testTenantDoesNotHaveLandlordRole() throws Exception {
        String token = generateToken("u-tenant-01", List.of("ROLE_TENANT"));
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/landlord/appointments");
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthFilter.doFilter(req, res, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertThat(auth.getAuthorities()).noneMatch(a -> a.getAuthority().equals("ROLE_LANDLORD"));
    }

    @Test
    @DisplayName("MODERATOR: Request with ROLE_MODERATOR lacks ROLE_LANDLORD")
    void testModeratorDoesNotHaveLandlordRole() throws Exception {
        String token = generateToken("u-mod-01", List.of("ROLE_MODERATOR"));
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/landlord/appointments");
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthFilter.doFilter(req, res, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertThat(auth.getAuthorities()).noneMatch(a -> a.getAuthority().equals("ROLE_LANDLORD"));
    }

    @Test
    @DisplayName("ADMIN: Request with ROLE_ADMIN lacks ROLE_LANDLORD")
    void testAdminDoesNotHaveLandlordRole() throws Exception {
        String token = generateToken("u-admin-01", List.of("ROLE_ADMIN"));
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/landlord/appointments");
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthFilter.doFilter(req, res, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertThat(auth.getAuthorities()).noneMatch(a -> a.getAuthority().equals("ROLE_LANDLORD"));
    }

    @Test
    @DisplayName("Expired JWT token is rejected: SecurityContext is cleared")
    void testExpiredTokenRejected() throws Exception {
        String expiredToken = Jwts.builder()
                .subject("u-landlord-01")
                .claim("roles", "ROLE_LANDLORD")
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .header().keyId("cozynest-identity-key-1").and()
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/landlord/appointments");
        req.addHeader("Authorization", "Bearer " + expiredToken);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthFilter.doFilter(req, res, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("JWT signed with unknown foreign key is rejected")
    void testWrongSignatureRejected() throws Exception {
        java.security.KeyPair foreignPair = java.security.KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String forgedToken = Jwts.builder()
                .subject("u-landlord-01")
                .claim("roles", "ROLE_LANDLORD")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .header().keyId("fake-key").and()
                .signWith(foreignPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/landlord/appointments");
        req.addHeader("Authorization", "Bearer " + forgedToken);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthFilter.doFilter(req, res, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("JWT with alg=none is rejected")
    void testAlgNoneRejected() throws Exception {
        String algNoneToken = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJzdWIiOiJ1LWxhbmRsb3JkLTAxIiwicm9sZXMiOiJMQU5ETE9SRCJ9.";

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/landlord/appointments");
        req.addHeader("Authorization", "Bearer " + algNoneToken);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthFilter.doFilter(req, res, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
