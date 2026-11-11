package vn.cozynest.admin.infrastructure.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.cozynest.common.security.DownstreamJwtVerifier;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class AdminSecurityMatrixTest {

    private static final String PRIVATE_KEY_PEM =
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDnoGWqlO0JAE2qksnCEvnRTqkwzFS6QuVyQ0EF+dyLtMCZgNj0Gz9/0sQAZRjfAJyH5w353P6h+jcT8A74BfvERvJXD7jP2bcrkYmBRDaOYgxQGQC7A+IJOZb8dpNxynhJuHmi7tZgtVpC6zAjAIprVajyCLJzLRSctYHO8qJyRP70UaMfPh9ANJwWcbGnW1aGp8DMlQYCHsXVZPepG1v4cbqtnEwKM0dEaVu/LFM5YwX/TNqn4KsOIfCW4JZiYcnlG1ryfEIPHxuI0uJOG8ungOHquSbDYp+/9mcINeJ8ZUxXJmWsrBU0W8VOrXcOIJhXzUxdQwmgvpmIpn2uQduDAgMBAAECggEAIAq2RbS4XahnFhOcSXaqjGPfT9rUVbfvWbEoC5WXBtSIWDPUz1/9lscnSg6yNBc3kC1MZ6ZEq0+b0uxAhdr+Cxbta1HHdSlfB23oobM4ANZbcbEjwrs0x6Xp1x7oIrOplDkqynlTuqkbxWoXjKAEhfihmGnPldNEqLELNf+mrTvOwiR3vLKNPkIbhoPWU0Or7t0XmkwZ9ku02EfisqT+nAgZufOiLU+AXEcrfEcVK0TuMKT7B4UcQw8tjDQtgBKsC4+/vxgPk1O3aMru720fSx8MntToLvAkq2nFZ/t5okROij7VBnIQeGXp84Bdmpzu7Ac+ji9Ls4omW0zQ25S5QQKBgQD+JW0QH8if+lXX2M7/ECiylCyLcze+lv3LYp3tqV3wrwUS4vACZJWau7PmHM/wyauXDrTyUn90pFoSkTRj3kr4QyMc6dGdM0fgFmp+ztrDlqM6XRqaVpsW2hhm+2umDtaonBCrYRXlvTDePi6ekbniZ+oQDNt8iMz2nWj78HXMkQKBgQDpUOtpOusxM7QiXdgZV/I5s/3Dy613Pcn5mgG+wnk3+pDOXMqN9GhCFdo7Ms6mBtcTEUJbhyFKD8abkZ/zXsEks1AxaorxV8Qt72Bu84iteTiy9ubOuwtQxkkNnGFfkAIqAxufJrv/ZHaujLshJCwo1gR7lPgSvuMuij1yJKZA0wKBgFYh0wOq8fCeA+fiV80D6fcT1sDwLI1QWPVMF+ALDYZbmqma9eKpujY1tTOgwiyF/Lx2RIb3xLCAXvJ5+bp7BQd8oXLUH7FaRtrAXhlZ+7Rb4jgOg8yY23j0M177iLE/yCpeshuTDLvO0bgQdPZSxx/fPelwohZjuqs+D+2KwjGxAoGBAIYGnOSbGuXtrImdXPjaA2RQV/L0epYn4XFHnb8GGfolLq/Mr/Kxtn5X7T/aZMkM3tmuLW9EhcP0ogNVuQtloA/EDcoFSQFwiFpWyw+RSC9WllJ/8IBLBCoHbYmrDmiF4iheFqzq5E3GWULVxt032UIwBu0y2vpxuzFN6hQ6p2JPAoGAbLRSNpK6wOY88iCSCH8pFv3hRbfEQUWlcNbQ/1FmueiQaCr4ucq9LCat4W3LjKth9kqVvlL6pqEp04U1FD0YqDZpT0CVWG0eWqgHz95gDc4HwnuaURrsyjBrI9GB/m3zyGbggsbVaWDFFgOQv6VPCN0wZrPpRg+zdvOuTy/qbco=";

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void setsAdminAuthenticationForValidTokenAndRejectsMissingOrInvalidTokens() throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter("");
        byte[] privBytes = Base64.getDecoder().decode(PRIVATE_KEY_PEM.replaceAll("\\s+", ""));
        RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privBytes));

        String token = Jwts.builder()
                .header().keyId(DownstreamJwtVerifier.DEFAULT_KEY_ID).and()
                .subject("admin-1")
                .audience().add(DownstreamJwtVerifier.EXPECTED_AUDIENCE).and()
                .claim("roles", "ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 600_000))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/admin/disputes");
        req.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority").containsExactly("ROLE_ADMIN");
    }
}

