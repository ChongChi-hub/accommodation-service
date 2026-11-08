package vn.cozynest.identity.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.cozynest.identity.domain.exception.IdentityDomainException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyValidatorTest {

    @Test
    @DisplayName("Reject password shorter than 8 characters")
    void shouldRejectShortPassword() {
        assertThatThrownBy(() -> PasswordPolicyValidator.validate("short", "user@test.vn"))
                .isInstanceOf(IdentityDomainException.class)
                .hasMessageContaining("tối thiểu 8 ký tự");
    }

    @Test
    @DisplayName("Reject password longer than 72 bytes due to BCrypt truncation boundary")
    void shouldRejectPasswordExceeding72Bytes() {
        String longPassword73Ascii = "A".repeat(73);
        assertThatThrownBy(() -> PasswordPolicyValidator.validate(longPassword73Ascii, "user@test.vn"))
                .isInstanceOf(IdentityDomainException.class)
                .hasMessageContaining("72 byte");

        // 37 Vietnamese characters (each 2-3 bytes) easily exceed 72 bytes
        String longVietnamesePassword = "MậtKhẩuBảoMậtRấtDàiChoHệThốngPhòngTrọCựcKỳAnToàn123";
        assertThatThrownBy(() -> PasswordPolicyValidator.validate(longVietnamesePassword, "user@test.vn"))
                .isInstanceOf(IdentityDomainException.class)
                .hasMessageContaining("72 byte");
    }

    @Test
    @DisplayName("Accept valid password exactly 72 bytes or less")
    void shouldAcceptValidPasswordWithin72Bytes() {
        String exactly72Bytes = "A".repeat(72);
        assertThatCode(() -> PasswordPolicyValidator.validate(exactly72Bytes, "user@test.vn"))
                .doesNotThrowAnyException();

        assertThatCode(() -> PasswordPolicyValidator.validate("ValidP@ssw0rd2026", "user@test.vn"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Reject common dictionary passwords")
    void shouldRejectCommonPasswords() {
        assertThatThrownBy(() -> PasswordPolicyValidator.validate("password123", "user@test.vn"))
                .isInstanceOf(IdentityDomainException.class)
                .hasMessageContaining("phổ biến");
    }

    @Test
    @DisplayName("Reject password matching email address or local-part")
    void shouldRejectPasswordMatchingEmail() {
        assertThatThrownBy(() -> PasswordPolicyValidator.validate("user@test.vn", "user@test.vn"))
                .isInstanceOf(IdentityDomainException.class)
                .hasMessageContaining("địa chỉ email");

        assertThatThrownBy(() -> PasswordPolicyValidator.validate("cuongle123", "cuongle123@test.vn"))
                .isInstanceOf(IdentityDomainException.class)
                .hasMessageContaining("tên người dùng email");
    }
}
