package com.service.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateReportRequest {
    @NotBlank(message = "targetType không được để trống")
    private String targetType;

    @NotBlank(message = "targetId không được để trống")
    private String targetId;

    @NotBlank(message = "reason không được để trống")
    private String reason;

    private String description;
}

