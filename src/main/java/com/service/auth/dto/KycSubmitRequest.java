package com.service.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KycSubmitRequest {
    @NotBlank(message = "Thiếu ảnh mặt trước CCCD")
    private String idCardFront;

    @NotBlank(message = "Thiếu ảnh mặt sau CCCD")
    private String idCardBack;

    private String selfieUrl;
}

