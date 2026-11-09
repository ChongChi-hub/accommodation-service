package vn.cozynest.identity.domain.service;

import vn.cozynest.identity.domain.exception.IdentityDomainException;

import java.nio.charset.StandardCharsets;
import java.util.Set;

public final class PasswordPolicyValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_BYTES = 72;

    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "12345678", "123456789", "1234567890", "password", "password123",
            "admin123", "qwertyui", "qwerty123", "11111111", "00000000",
            "iloveyou", "welcome1", "welcome123", "cozynest123", "toam123456"
    );

    private PasswordPolicyValidator() {}

    public static void validate(String password, String email) {
        validate(password, email, null);
    }

    public static void validate(String password, String email, String fullName) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new IdentityDomainException(
                    "Mật khẩu phải có tối thiểu " + MIN_LENGTH + " ký tự",
                    "ERR_AUTH_PASSWORD_TOO_SHORT"
            );
        }

        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new IdentityDomainException(
                    "Mật khẩu không được vượt quá " + MAX_BYTES + " byte (giới hạn bảo mật BCrypt)",
                    "ERR_AUTH_PASSWORD_TOO_LONG"
            );
        }

        String lowerPass = password.toLowerCase().trim();
        if (COMMON_PASSWORDS.contains(lowerPass)) {
            throw new IdentityDomainException(
                    "Mật khẩu quá phổ biến, dễ đoán. Vui lòng chọn mật khẩu phức tạp hơn.",
                    "ERR_AUTH_PASSWORD_TOO_COMMON"
            );
        }

        // Check repetitive characters (e.g., "aaaaaaaa", "11111111")
        if (lowerPass.chars().distinct().count() == 1) {
            throw new IdentityDomainException(
                    "Mật khẩu không được là chuỗi ký tự lặp lại",
                    "ERR_AUTH_PASSWORD_REPETITIVE"
            );
        }

        // Check sequential characters (e.g., "12345678", "87654321")
        if (isSequential(lowerPass)) {
            throw new IdentityDomainException(
                    "Mật khẩu không được là chuỗi số thứ tự đơn điệu",
                    "ERR_AUTH_PASSWORD_REPETITIVE"
            );
        }

        if (email != null && !email.isBlank()) {
            String lowerEmail = email.toLowerCase().trim();
            if (lowerPass.equals(lowerEmail)) {
                throw new IdentityDomainException(
                        "Mật khẩu không được trùng với địa chỉ email",
                        "ERR_AUTH_PASSWORD_EQUALS_EMAIL"
                );
            }
            int atIndex = lowerEmail.indexOf('@');
            if (atIndex > 0) {
                String emailPrefix = lowerEmail.substring(0, atIndex);
                if (lowerPass.equals(emailPrefix) || (emailPrefix.length() >= 4 && lowerPass.contains(emailPrefix))) {
                    throw new IdentityDomainException(
                            "Mật khẩu không được trùng với tên người dùng email",
                            "ERR_AUTH_PASSWORD_EQUALS_EMAIL"
                    );
                }
            }
        }

        if (fullName != null && !fullName.isBlank()) {
            String lowerName = fullName.toLowerCase().trim();
            if (lowerPass.equals(lowerName) || (lowerName.length() >= 3 && lowerPass.contains(lowerName))) {
                throw new IdentityDomainException(
                        "Mật khẩu không được chứa họ tên của bạn",
                        "ERR_AUTH_PASSWORD_CONTAINS_NAME"
                );
            }
        }
    }

    private static boolean isSequential(String s) {
        if (s == null || s.length() < 6) return false;
        boolean inc = true;
        boolean dec = true;
        for (int i = 0; i < s.length() - 1; i++) {
            char c1 = s.charAt(i);
            char c2 = s.charAt(i + 1);
            if (c2 - c1 != 1) inc = false;
            if (c1 - c2 != 1) dec = false;
        }
        return inc || dec;
    }
}
