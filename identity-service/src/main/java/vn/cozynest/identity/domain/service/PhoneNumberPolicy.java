package vn.cozynest.identity.domain.service;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates, normalizes, and masks Vietnamese mobile phone numbers, blocking disposable/VoIP prefixes.
 */
public final class PhoneNumberPolicy {

    private static final Pattern VN_MOBILE_PATTERN = Pattern.compile("^0(3|5|7|8|9)[0-9]{8}$");
    private static final Pattern REPEATED_BODY_PATTERN = Pattern.compile("^0[0-9](\\d)\\1{7}$");
    private static final Set<String> BLOCKED_VOIP_PREFIXES = Set.of("0599", "0998", "0999");

    private PhoneNumberPolicy() {
    }

    public static String normalizeAndValidate(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new IllegalArgumentException("So dien thoai khong duoc de trong");
        }
        String compact = rawPhone.replaceAll("[\\s.-]", "");
        if (compact.startsWith("+84")) {
            compact = "0" + compact.substring(3);
        } else if (compact.startsWith("84") && compact.length() == 11) {
            compact = "0" + compact.substring(2);
        }

        if (!VN_MOBILE_PATTERN.matcher(compact).matches()) {
            throw new IllegalArgumentException("So dien thoai di dong Viet Nam khong hop le");
        }
        String prefix4 = compact.substring(0, 4);
        if (BLOCKED_VOIP_PREFIXES.contains(prefix4)
                || REPEATED_BODY_PATTERN.matcher(compact).matches()
                || "0900000000".equals(compact)
                || "0911111111".equals(compact)) {
            throw new IllegalArgumentException("Khong ho tro dau so ao hoac so dien thoai khong hop le");
        }
        return compact;
    }

    public static String mask(String phone) {
        if (phone == null || phone.length() < 6) {
            return "****";
        }
        String normalized = phone.trim();
        return normalized.substring(0, 3) + "****" + normalized.substring(normalized.length() - 3);
    }
}

