package vn.cozynest.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyPhoneOtpRequest(
        @NotBlank(message = "Số điện thoại không được để trống")
        String phoneNumber,

        @NotBlank(message = "Mã OTP không được để trống")
        @Pattern(regexp = "^[0-9]{6}$", message = "Mã OTP phải gồm 6 chữ số")
        String otp
) {}

