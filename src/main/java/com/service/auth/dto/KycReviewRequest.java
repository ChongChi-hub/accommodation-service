package com.service.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KycReviewRequest {
    @NotBlank(message = "Thiếu trạng thái xét duyệt ('approved' hoặc 'rejected')")
    private String status;
}

