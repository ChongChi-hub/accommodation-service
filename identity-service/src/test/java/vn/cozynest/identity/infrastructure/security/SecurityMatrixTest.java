package vn.cozynest.identity.infrastructure.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.model.UserRoleType;
import vn.cozynest.identity.interfaces.rest.JwksController;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SecurityMatrixTest {

    private RsaKeyProvider rsaKeyProvider;
    private JwtTokenProvider jwtTokenProvider;
    private JwtAuthFilter jwtAuthFilter;
    private JwksController jwksController;

    @BeforeEach
    void setUp() {
        rsaKeyProvider = new RsaKeyProvider();
        jwtTokenProvider = new JwtTokenProvider(rsaKeyProvider);
        jwtAuthFilter = new JwtAuthFilter(rsaKeyProvider);
        jwksController = new JwksController(rsaKeyProvider);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Public JWKS endpoint exposes valid RS256 RSA keys with n and e parameters")
    @SuppressWarnings("unchecked")
    void testJwksEndpointExposesRsaPublicKey() {
        Map<String, Object> jwks = jwksController.getJwks();
        assertThat(jwks).containsKey("keys");
        var keys = (java.util.List<?>) jwks.get("keys");
        assertThat(keys).hasSize(1);
        Map<String, Object> keyMap = (Map<String, Object>) keys.get(0);
        assertThat(keyMap.get("kty")).isEqualTo("RSA");
        assertThat(keyMap.get("use")).isEqualTo("sig");
        assertThat(keyMap.get("alg")).isEqualTo("RS256");
        assertThat(keyMap.get("kid")).isEqualTo("cozynest-identity-key-1");
        assertThat(keyMap).containsKey("n");
        assertThat(keyMap).containsKey("e");
    }

    @Test
    @DisplayName("Request without token leaves SecurityContext empty (GUEST)")
    void testGuestRequestHasNoAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        jwtAuthFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
    }

    @Test
    @DisplayName("Request with invalid or tampered JWT is rejected (SecurityContext empty)")
    void testTamperedTokenIsRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader("Authorization", "Bearer eyJhbGciOiJSUzI1NiJ9.invalid.signature");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        jwtAuthFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
    }

    @Test
    @DisplayName("Role Matrix: TENANT token sets ROLE_TENANT authority")
    void testTenantAuthentication() throws Exception {
        User tenant = User.createNew("tenant@cozynest.vn", "0901234567", "hash", "Nguyễn Thuê", UserRoleType.TENANT);
        String token = jwtTokenProvider.generateAccessToken(tenant);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/tenant/bookings");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        jwtAuthFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_TENANT"));
        assertThat(auth.getAuthorities()).noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("Role Matrix: LANDLORD token sets ROLE_LANDLORD authority")
    void testLandlordAuthentication() throws Exception {
        User landlord = User.createNew("landlord@cozynest.vn", "0901234568", "hash", "Nguyễn Chủ Trọ", UserRoleType.LANDLORD);
        String token = jwtTokenProvider.generateAccessToken(landlord);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/landlord/rooms");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        jwtAuthFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_LANDLORD"));
    }

    @Test
    @DisplayName("Role Matrix: MODERATOR token sets ROLE_MODERATOR authority")
    void testModeratorAuthentication() throws Exception {
        User mod = User.createNew("mod@cozynest.vn", "0901234569", "hash", "Kiểm Duyệt", UserRoleType.MODERATOR);
        String token = jwtTokenProvider.generateAccessToken(mod);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/moderator/queue");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        jwtAuthFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_MODERATOR"));
    }

    @Test
    @DisplayName("Role Matrix: ADMIN token sets ROLE_ADMIN authority")
    void testAdminAuthentication() throws Exception {
        User admin = User.createNew("admin@cozynest.vn", "0901234560", "hash", "Quản Trị Viên", UserRoleType.ADMIN);
        String token = jwtTokenProvider.generateAccessToken(admin);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/users");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        jwtAuthFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
