package com.service.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Vui lòng cung cấp email")
    private String email;

    @NotBlank(message = "Vui lòng cung cấp mật khẩu")
    private String password;
}

