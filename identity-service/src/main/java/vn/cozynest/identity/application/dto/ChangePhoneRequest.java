package vn.cozynest.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePhoneRequest(
        @NotBlank(message = "Số điện thoại mới không được để trống")
        @Pattern(regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại Việt Nam không hợp lệ")
        String newPhoneNumber,

        @NotBlank(message = "Mã OTP số mới không được để trống")
        @Pattern(regexp = "^[0-9]{6}$", message = "Mã OTP phải gồm 6 chữ số")
        String newOtp,

        @Pattern(regexp = "^[0-9]{6}$", message = "Mã OTP số cũ phải gồm 6 chữ số")
        String oldOtp
) {}

