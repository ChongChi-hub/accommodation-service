package vn.cozynest.identity.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class CookieCsrfProtectionFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_ORIGINS = Set.of(
            "http://localhost:3000",
            "http://127.0.0.1:3000",
            "https://cozynest.vn"
    );

    private static final Set<String> STATE_CHANGING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();

        if (STATE_CHANGING_METHODS.contains(method)) {
            boolean hasAuthHeader = request.getHeader("Authorization") != null;
            boolean hasCookie = hasAuthCookie(request);

            // If authenticating exclusively via cookie (no Authorization header)
            if (hasCookie && !hasAuthHeader) {
                String origin = request.getHeader("Origin");
                String secFetchSite = request.getHeader("Sec-Fetch-Site");

                if (origin != null && !origin.isBlank()) {
                    if (!ALLOWED_ORIGINS.contains(origin)) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF origin validation failed");
                        return;
                    }
                } else if ("cross-site".equalsIgnoreCase(secFetchSite)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Cross-site request blocked by CSRF policy");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean hasAuthCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return false;
        for (Cookie c : cookies) {
            if ("cozynest_token".equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                return true;
            }
        }
        return false;
    }
}

