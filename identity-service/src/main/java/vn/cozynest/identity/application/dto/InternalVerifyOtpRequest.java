package vn.cozynest.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InternalVerifyOtpRequest(
        @NotBlank(message = "Ma OTP khong duoc de trong")
        @Pattern(regexp = "^[0-9]{6,8}$", message = "Ma OTP phai co 6 hoac 8 chu so")
        String otpCode
) {}

