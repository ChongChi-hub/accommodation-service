package vn.cozynest.booking.domain.model;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Generates human-readable, collision-resistant order codes in format:
 * TOAM-YYMMDD-XXXXXX (Crockford Base32 without ambiguous I, L, O, U).
 */
public final class OrderCodeGenerator {

    private static final char[] CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter YYMMDD = DateTimeFormatter.ofPattern("yyMMdd");

    private OrderCodeGenerator() {}

    public static String generate() {
        return generate(LocalDate.now(ZoneOffset.UTC));
    }

    public static String generate(LocalDate date) {
        StringBuilder suffix = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            suffix.append(CROCKFORD[RANDOM.nextInt(CROCKFORD.length)]);
        }
        return "TOAM-" + date.format(YYMMDD) + "-" + suffix;
    }

    public static String generateCheckinCode() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }
}

