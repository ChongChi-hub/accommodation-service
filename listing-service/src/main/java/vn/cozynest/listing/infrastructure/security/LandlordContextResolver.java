package vn.cozynest.listing.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import vn.cozynest.common.security.DownstreamJwtVerifier;

import java.util.Map;

@Component
public class LandlordContextResolver {

    private static final Map<String, Long> DEMO_LANDLORD_MAPPING = Map.ofEntries(
            Map.entry("u-landlord-01", 1L),
            Map.entry("landlord@cozynest.vn", 1L),
            Map.entry("u-landlord-02", 2L),
            Map.entry("landlord2@cozynest.vn", 2L),
            Map.entry("u-landlord-03", 3L),
            Map.entry("landlord3@cozynest.vn", 3L),
            Map.entry("u-landlord-04", 4L),
            Map.entry("landlord4@cozynest.vn", 4L),
            Map.entry("u-landlord-05", 5L),
            Map.entry("landlord5@cozynest.vn", 5L),
            Map.entry("u-landlord-06", 6L),
            Map.entry("landlord6@cozynest.vn", 6L)
    );

    public Long getCurrentLandlordId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }

        String principal = auth.getName();
        if (principal == null) {
            return null;
        }

        if (DEMO_LANDLORD_MAPPING.containsKey(principal)) {
            return DEMO_LANDLORD_MAPPING.get(principal);
        }

        try {
            return Long.parseLong(principal);
        } catch (NumberFormatException ignored) {
            long hash = Math.abs((long) principal.hashCode());
            return hash > 100 ? hash : hash + 1000L;
        }
    }

    public String getCurrentActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return "anonymous";
        }
        return auth.getName();
    }

    public String getCurrentLandlordName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof DownstreamJwtVerifier.VerifiedPrincipal vp) {
            String name = vp.claims().get("fullName", String.class);
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        return "Chủ trọ";
    }

    public String getCurrentLandlordPhone() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof DownstreamJwtVerifier.VerifiedPrincipal vp) {
            String phone = vp.claims().get("phone", String.class);
            if (phone != null && !phone.isBlank()) {
                return phone;
            }
        }
        return "0901234567";
    }

    public String getCurrentLandlordEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof DownstreamJwtVerifier.VerifiedPrincipal vp) {
            String email = vp.claims().get("email", String.class);
            if (email != null && !email.isBlank()) {
                return email;
            }
        }
        return null;
    }

    public boolean isCurrentLandlordFaceVerified() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof DownstreamJwtVerifier.VerifiedPrincipal vp) {
            Boolean verified = vp.claims().get("face_verified", Boolean.class);
            if (verified != null) {
                return verified;
            }
        }
        String actor = getCurrentActorId();
        if (DEMO_LANDLORD_MAPPING.containsKey(actor) || "u-landlord-01".equals(actor)) {
            return true;
        }
        return false;
    }

    public boolean isPrivilegedModeratorOrAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority().toUpperCase())
                .anyMatch(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_MODERATOR") || r.equals("ADMIN") || r.equals("MODERATOR"));
    }

    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority().toUpperCase())
                .anyMatch(r -> r.equals("ROLE_ADMIN") || r.equals("ADMIN"));
    }

    public boolean isModerator() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority().toUpperCase())
                .anyMatch(r -> r.equals("ROLE_MODERATOR") || r.equals("MODERATOR"));
    }

    public String getCurrentRole() {
        if (isAdmin()) return "ADMIN";
        if (isModerator()) return "MODERATOR";
        return "LANDLORD";
    }
}
