package vn.cozynest.identity.domain.service;

import vn.cozynest.identity.domain.exception.IdentityDomainException;

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
        if (password == null || password.length() < MIN_LENGTH) {
            throw new IdentityDomainException(
                    "Mật khẩu phải có tối thiểu " + MIN_LENGTH + " ký tự",
                    "ERR_AUTH_PASSWORD_TOO_SHORT"
            );
        }

        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_BYTES) {
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

        if (email != null && !email.isBlank()) {
            String lowerEmail = email.toLowerCase().trim();
            if (lowerPass.equals(lowerEmail)) {
                throw new IdentityDomainException(
                        "Mật khẩu không được trùng với địa chỉ email",
                        "ERR_AUTH_PASSWORD_EQUALS_EMAIL"
                );
            }
            int atIndex = lowerEmail.indexOf('@');
            if (atIndex > 2) {
                String emailPrefix = lowerEmail.substring(0, atIndex);
                if (lowerPass.equals(emailPrefix)) {
                    throw new IdentityDomainException(
                            "Mật khẩu không được trùng với tên người dùng email",
                            "ERR_AUTH_PASSWORD_EQUALS_EMAIL"
                    );
                }
            }
        }
    }
}

