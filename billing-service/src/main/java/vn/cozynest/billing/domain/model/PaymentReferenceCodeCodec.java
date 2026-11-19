package vn.cozynest.billing.domain.model;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PaymentReferenceCodeCodec {
    private static final String PREFIX = "TOAM";
    private static final String ALPHABET = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int RANDOM_LEN = 15; // TOAM + 15 chars = 19 chars (<= 23 VietQR max)
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern TOAM_REF_PATTERN = Pattern.compile("(TOAM[0-9A-Z]{10,19})");
    private static final Pattern LEGACY_PAY_STRIPPED_PATTERN = Pattern.compile("(PAY[0-9]{10,15}[0-9A-F]{4})");

    private PaymentReferenceCodeCodec() {
    }

    public static String generateReferenceCode() {
        StringBuilder sb = new StringBuilder(PREFIX.length() + RANDOM_LEN);
        sb.append(PREFIX);
        for (int i = 0; i < RANDOM_LEN; i++) {
            sb.append(ALPHABET.charAt(SECURE_RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public static String normalizeAlphanumericUpper(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    public static Optional<String> extractReferenceFromBankContent(String rawBankContent) {
        String norm = normalizeAlphanumericUpper(rawBankContent);
        if (norm.isEmpty()) {
            return Optional.empty();
        }
        Matcher toamMatcher = TOAM_REF_PATTERN.matcher(norm);
        if (toamMatcher.find()) {
            return Optional.of(toamMatcher.group(1));
        }
        Matcher legacyMatcher = LEGACY_PAY_STRIPPED_PATTERN.matcher(norm);
        if (legacyMatcher.find()) {
            return Optional.of(legacyMatcher.group(1));
        }
        return Optional.empty();
    }

    public static boolean matchesPaymentCode(String rawBankContent, String storedPaymentCode) {
        if (rawBankContent == null || storedPaymentCode == null) {
            return false;
        }
        String normContent = normalizeAlphanumericUpper(rawBankContent);
        String normCode = normalizeAlphanumericUpper(storedPaymentCode);
        return !normCode.isEmpty() && normContent.contains(normCode);
    }

    public static String maskAccountNumber(String rawAccount) {
        if (rawAccount == null || rawAccount.isBlank()) {
            return "****";
        }
        String digits = rawAccount.replaceAll("[^A-Za-z0-9]", "");
        if (digits.length() <= 4) {
            return "****";
        }
        return "******" + digits.substring(digits.length() - 4);
    }
}

