package vn.cozynest.listing;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.listing.application.dto.BookmarkStatusDto;
import vn.cozynest.listing.application.dto.RoomDto;
import vn.cozynest.listing.application.service.BookmarkService;
import vn.cozynest.listing.infrastructure.security.JwtAuthFilter;
import vn.cozynest.listing.infrastructure.web.BookmarkController;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookmarkSecurityMatrixTest {

    private JwtAuthFilter jwtAuthFilter;
    private RSAPrivateKey privateKey;

    @Mock
    private BookmarkService bookmarkService;

    @InjectMocks
    private BookmarkController bookmarkController;

    private static final String PRIVATE_KEY_PEM =
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDnoGWqlO0JAE2qksnCEvnRTqkwzFS6QuVyQ0EF+dyLtMCZgNj0Gz9/0sQAZRjfAJyH5w353P6h+jcT8A74BfvERvJXD7jP2bcrkYmBRDaOYgxQGQC7A+IJOZb8dpNxynhJuHmi7tZgtVpC6zAjAIprVajyCLJzLRSctYHO8qJyRP70UaMfPh9ANJwWcbGnW1aGp8DMlQYCHsXVZPepG1v4cbqtnEwKM0dEaVu/LFM5YwX/TNqn4KsOIfCW4JZiYcnlG1ryfEIPHxuI0uJOG8ungOHquSbDYp+/9mcINeJ8ZUxXJmWsrBU0W8VOrXcOIJhXzUxdQwmgvpmIpn2uQduDAgMBAAECggEAIAq2RbS4XahnFhOcSXaqjGPfT9rUVbfvWbEoC5WXBtSIWDPUz1/9lscnSg6yNBc3kC1MZ6ZEq0+b0uxAhdr+Cxbta1HHdSlfB23oobM4ANZbcbEjwrs0x6Xp1x7oIrOplDkqynlTuqkbxWoXjKAEhfihmGnPldNEqLELNf+mrTvOwiR3vLKNPkIbhoPWU0Or7t0XmkwZ9ku02EfisqT+nAgZufOiLU+AXEcrfEcVK0TuMKT7B4UcQw8tjDQtgBKsC4+/vxgPk1O3aMru720fSx8MntToLvAkq2nFZ/t5okROij7VBnIQeGXp84Bdmpzu7Ac+ji9Ls4omW0zQ25S5QQKBgQD+JW0QH8if+lXX2M7/ECiylCyLcze+lv3LYp3tqV3wrwUS4vACZJWau7PmHM/wyauXDrTyUn90pFoSkTRj3kr4QyMc6dGdM0fgFmp+ztrDlqM6XRqaVpsW2hhm+2umDtaonBCrYRXlvTDePi6ekbniZ+oQDNt8iMz2nWj78HXMkQKBgQDpUOtpOusxM7QiXdgZV/I5s/3Dy613Pcn5mgG+wnk3+pDOXMqN9GhCFdo7Ms6mBtcTEUJbhyFKD8abkZ/zXsEks1AxaorxV8Qt72Bu84iteTiy9ubOuwtQxkkNnGFfkAIqAxufJrv/ZHaujLshJCwo1gR7lPgSvuMuij1yJKZA0wKBgFYh0wOq8fCeA+fiV80D6fcT1sDwLI1QWPVMF+ALDYZbmqma9eKpujY1tTOgwiyF/Lx2RIb3xLCAXvJ5+bp7BQd8oXLUH7FaRtrAXhlZ+7Rb4jgOg8yY23j0M177iLE/yCpeshuTDLvO0bgQdPZSxx/fPelwohZjuqs+D+2KwjGxAoGBAIYGnOSbGuXtrImdXPjaA2RQV/L0epYn4XFHnb8GGfolLq/Mr/Kxtn5X7T/aZMkM3tmuLW9EhcP0ogNVuQtloA/EDcoFSQFwiFpWyw+RSC9WllJ/8IBLBCoHbYmrDmiF4iheFqzq5E3GWULVxt032UIwBu0y2vpxuzFN6hQ6p2JPAoGAbLRSNpK6wOY88iCSCH8pFv3hRbfEQUWlcNbQ/1FmueiQaCr4ucq9LCat4W3LjKth9kqVvlL6pqEp04U1FD0YqDZpT0CVWG0eWqgHz95gDc4HwnuaURrsyjBrI9GB/m3zyGbggsbVaWDFFgOQv6VPCN0wZrPpRg+zdvOuTy/qbco=";

    @BeforeEach
    void setUp() throws Exception {
        jwtAuthFilter = new JwtAuthFilter("");
        KeyFactory kf = KeyFactory.getInstance("RSA");
        byte[] privBytes = Base64.getDecoder().decode(PRIVATE_KEY_PEM.replaceAll("\\s+", ""));
        this.privateKey = (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String generateToken(String userId, List<String> roles) {
        return Jwts.builder()
                .subject(userId)
                .audience().add("cozynest-web").and()
                .claim("roles", String.join(",", roles))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .header().keyId("cozynest-identity-key-1").and()
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    // --- 1. GUEST / FILTER LEVEL TESTS ---

    @Test
    @DisplayName("1.1 Guest: Request without token produces null Authentication")
    void testGuestHasNoAuth() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/bookmarks");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthFilter.doFilter(req, res, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("1.2 Guest: Can query room bookmark status anonymously")
    void testGuestQueryStatus() {
        when(bookmarkService.getStatus(null, 101L))
                .thenReturn(BookmarkStatusDto.builder().roomId(101L).isBookmarked(false).bookmarkCount(5).build());

        ResponseEntity<ApiResponse<BookmarkStatusDto>> response = bookmarkController.getStatus(101L, null);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().isBookmarked()).isFalse();
        assertThat(response.getBody().getData().getBookmarkCount()).isEqualTo(5);
    }

    // --- 2. TENANT ROLE CONTROLLER TESTS ---

    @Test
    @DisplayName("2.1 Tenant: Authenticated via JWT token and add bookmark returns 200")
    void testTenantAddBookmark() throws Exception {
        String token = generateToken("u-tenant-01", List.of("TENANT"));
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/bookmarks/101");
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthFilter.doFilter(req, res, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertThat(auth.getName()).isEqualTo("u-tenant-01");

        when(bookmarkService.addBookmark("u-tenant-01", 101L))
                .thenReturn(BookmarkStatusDto.builder().roomId(101L).isBookmarked(true).bookmarkCount(1).build());

        ResponseEntity<ApiResponse<BookmarkStatusDto>> response = bookmarkController.addBookmark(101L, auth);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().isBookmarked()).isTrue();
    }

    @Test
    @DisplayName("2.2 Tenant: Remove bookmark returns 200")
    void testTenantRemoveBookmark() {
        Authentication auth = new UsernamePasswordAuthenticationToken("u-tenant-01", null, List.of());
        when(bookmarkService.removeBookmark("u-tenant-01", 101L))
                .thenReturn(BookmarkStatusDto.builder().roomId(101L).isBookmarked(false).bookmarkCount(0).build());

        ResponseEntity<ApiResponse<BookmarkStatusDto>> response = bookmarkController.removeBookmark(101L, auth);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().isBookmarked()).isFalse();
    }

    @Test
    @DisplayName("2.3 Tenant: Get my bookmarks returns 200 list")
    void testTenantGetMyBookmarks() {
        Authentication auth = new UsernamePasswordAuthenticationToken("u-tenant-01", null, List.of());
        RoomDto room = RoomDto.builder().id(101L).title("Phòng 101").isBookmarked(true).build();
        when(bookmarkService.getMyBookmarks("u-tenant-01")).thenReturn(List.of(room));

        ResponseEntity<ApiResponse<List<RoomDto>>> response = bookmarkController.getMyBookmarks(auth);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).hasSize(1);
    }

    // --- 3. LANDLORD & ADMIN TESTS ---

    @Test
    @DisplayName("3.1 Landlord & Admin can also save bookmarks")
    void testOtherRolesCanBookmark() {
        Authentication landlordAuth = new UsernamePasswordAuthenticationToken("u-landlord-01", null, List.of());
        when(bookmarkService.addBookmark("u-landlord-01", 101L))
                .thenReturn(BookmarkStatusDto.builder().roomId(101L).isBookmarked(true).bookmarkCount(2).build());

        ResponseEntity<ApiResponse<BookmarkStatusDto>> res1 = bookmarkController.addBookmark(101L, landlordAuth);
        assertThat(res1.getStatusCode().value()).isEqualTo(200);

        Authentication adminAuth = new UsernamePasswordAuthenticationToken("u-admin-01", null, List.of());
        when(bookmarkService.addBookmark("u-admin-01", 101L))
                .thenReturn(BookmarkStatusDto.builder().roomId(101L).isBookmarked(true).bookmarkCount(3).build());

        ResponseEntity<ApiResponse<BookmarkStatusDto>> res2 = bookmarkController.addBookmark(101L, adminAuth);
        assertThat(res2.getStatusCode().value()).isEqualTo(200);
    }
}
