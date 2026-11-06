package com.service.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateConversationRequest {
    @NotBlank(message = "landlordId không được để trống")
    private String landlordId;

    private String roomId;
}

