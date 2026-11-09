package vn.cozynest.identity.application.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.cozynest.identity.domain.exception.IdentityDomainException;
import vn.cozynest.identity.domain.service.PasswordPolicyValidator;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthValidationVectorTest {

    private static JsonNode vectors;
    private static Validator beanValidator;

    @BeforeAll
    static void init() throws Exception {
        File f = new File("docs/auth_test_vectors.json");
        if (!f.exists()) {
            f = new File("../docs/auth_test_vectors.json");
        }
        assertThat(f).exists();
        ObjectMapper mapper = new ObjectMapper();
        vectors = mapper.readTree(f);

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        beanValidator = factory.getValidator();
    }

    @Test
    @DisplayName("Verify shared JSON vectors for email")
    void validateEmailVectors() {
        for (JsonNode valid : vectors.get("email").get("valid")) {
            RegisterCommand cmd = validBaseCommand();
            cmd.setEmail(valid.asText());
            Set<ConstraintViolation<RegisterCommand>> violations = beanValidator.validate(cmd);
            assertThat(violations)
                    .as("Expected valid email: " + valid.asText())
                    .isEmpty();
        }
        for (JsonNode invalid : vectors.get("email").get("invalid")) {
            RegisterCommand cmd = validBaseCommand();
            cmd.setEmail(invalid.asText());
            Set<ConstraintViolation<RegisterCommand>> violations = beanValidator.validate(cmd);
            assertThat(violations)
                    .as("Expected invalid email: " + invalid.asText())
                    .isNotEmpty();
        }
    }

    @Test
    @DisplayName("Verify shared JSON vectors for phone (optional)")
    void validatePhoneVectors() {
        for (JsonNode valid : vectors.get("phone").get("valid")) {
            RegisterCommand cmd = validBaseCommand();
            cmd.setPhone(valid.asText());
            Set<ConstraintViolation<RegisterCommand>> violations = beanValidator.validate(cmd);
            assertThat(violations)
                    .as("Expected valid phone: " + valid.asText())
                    .isEmpty();
        }
        for (JsonNode invalid : vectors.get("phone").get("invalid")) {
            RegisterCommand cmd = validBaseCommand();
            cmd.setPhone(invalid.asText());
            Set<ConstraintViolation<RegisterCommand>> violations = beanValidator.validate(cmd);
            assertThat(violations)
                    .as("Expected invalid phone: " + invalid.asText())
                    .isNotEmpty();
        }
    }

    @Test
    @DisplayName("Verify shared JSON vectors for fullName")
    void validateFullNameVectors() {
        for (JsonNode valid : vectors.get("fullName").get("valid")) {
            RegisterCommand cmd = validBaseCommand();
            cmd.setFullName(valid.asText());
            Set<ConstraintViolation<RegisterCommand>> violations = beanValidator.validate(cmd);
            assertThat(violations)
                    .as("Expected valid fullName: " + valid.asText())
                    .isEmpty();
        }
        for (JsonNode invalid : vectors.get("fullName").get("invalid")) {
            RegisterCommand cmd = validBaseCommand();
            cmd.setFullName(invalid.asText());
            Set<ConstraintViolation<RegisterCommand>> violations = beanValidator.validate(cmd);
            assertThat(violations)
                    .as("Expected invalid fullName: " + invalid.asText())
                    .isNotEmpty();
        }
    }

    @Test
    @DisplayName("Verify shared JSON vectors for password including repetitive, sequential, fullName & 72 UTF-8 byte boundary")
    void validatePasswordVectors() {
        String testEmail = "tenant@cozynest.vn";
        String testFullName = "Nguyễn Văn A";

        for (JsonNode valid : vectors.get("password").get("valid")) {
            String pwd = valid.asText();
            assertThatCode(() -> PasswordPolicyValidator.validate(pwd, testEmail, testFullName))
                    .as("Expected valid password: " + pwd)
                    .doesNotThrowAnyException();
        }

        for (JsonNode invalid : vectors.get("password").get("invalid")) {
            String pwd = invalid.asText();
            assertThatThrownBy(() -> PasswordPolicyValidator.validate(pwd, testEmail, testFullName))
                    .as("Expected invalid password: " + pwd)
                    .isInstanceOf(IdentityDomainException.class);
        }

        // Exact 72-byte UTF-8 boundary with multi-byte Vietnamese diacritics
        String exact72BytesVi = "TổẤmNhàĐẹpTổẤmNhàĐẹpTổẤmNhàĐẹpViệtNamxxxxxxxxx";
        assertThat(exact72BytesVi.getBytes(StandardCharsets.UTF_8).length).isEqualTo(72);
        assertThatCode(() -> PasswordPolicyValidator.validate(exact72BytesVi, testEmail, testFullName))
                .doesNotThrowAnyException();

        String exceed73BytesVi = exact72BytesVi + "x";
        assertThat(exceed73BytesVi.getBytes(StandardCharsets.UTF_8).length).isEqualTo(73);
        assertThatThrownBy(() -> PasswordPolicyValidator.validate(exceed73BytesVi, testEmail, testFullName))
                .isInstanceOf(IdentityDomainException.class);
    }

    private RegisterCommand validBaseCommand() {
        return RegisterCommand.builder()
                .email("valid@cozynest.vn")
                .phone("0901234567")
                .fullName("Nguyễn Văn A")
                .password("CorrectHorseBattery99")
                .role("TENANT")
                .build();
    }
}

