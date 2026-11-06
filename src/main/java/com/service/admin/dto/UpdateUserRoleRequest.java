package com.service.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUserRoleRequest {
    @NotBlank(message = "role không được để trống")
    private String role;
}

