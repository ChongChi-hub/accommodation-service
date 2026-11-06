package com.service.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Vui lòng nhập email")
    private String email;

    @NotBlank(message = "Vui lòng nhập mã OTP")
    private String otp;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới")
    private String newPassword;
}

