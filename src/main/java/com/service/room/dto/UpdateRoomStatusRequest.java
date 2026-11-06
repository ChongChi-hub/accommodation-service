package com.service.room.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateRoomStatusRequest {
    @NotBlank(message = "Thiếu status")
    private String status;
}

