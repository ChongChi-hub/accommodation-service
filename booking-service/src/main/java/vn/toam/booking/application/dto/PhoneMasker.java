package vn.toam.booking.application.dto;

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
}
