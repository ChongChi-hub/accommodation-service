package vn.toam.listing.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LandlordContextResolver {

    private static final Map<String, Long> DEMO_LANDLORD_MAPPING = Map.ofEntries(
            Map.entry("u-landlord-01", 1L),
            Map.entry("landlord@toam.vn", 1L),
            Map.entry("u-landlord-02", 2L),
            Map.entry("landlord2@toam.vn", 2L),
            Map.entry("u-landlord-03", 3L),
            Map.entry("landlord3@toam.vn", 3L),
            Map.entry("u-landlord-04", 4L),
            Map.entry("landlord4@toam.vn", 4L),
            Map.entry("u-landlord-05", 5L),
            Map.entry("landlord5@toam.vn", 5L),
            Map.entry("u-landlord-06", 6L),
            Map.entry("landlord6@toam.vn", 6L)
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
            if (principal.toLowerCase().contains("landlord")) {
                return 1L;
            }
            return null;
        }
    }

    public String getCurrentActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return "anonymous";
        }
        return auth.getName();
    }
}
