package vn.cozynest.booking.application.dto;

public final class PhoneMasker {

    private PhoneMasker() {}

    public static String mask(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String trimmed = phone.trim();
        if (trimmed.length() <= 6) {
            return trimmed;
        }
        return trimmed.substring(0, 3) + "••••" + trimmed.substring(trimmed.length() - 3);
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        String trimmed = email.trim();
        int atIdx = trimmed.indexOf('@');
        if (atIdx <= 1) {
            return "***" + (atIdx >= 0 ? trimmed.substring(atIdx) : "");
        }
        String local = trimmed.substring(0, atIdx);
        String domain = trimmed.substring(atIdx);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
}
