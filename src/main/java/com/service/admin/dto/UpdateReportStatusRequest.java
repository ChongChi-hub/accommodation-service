package com.service.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateReportStatusRequest {
    @NotBlank(message = "status không được để trống")
    private String status;
}

