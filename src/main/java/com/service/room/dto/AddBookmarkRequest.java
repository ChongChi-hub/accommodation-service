package com.service.room.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddBookmarkRequest {
    @NotBlank(message = "Thiếu roomId")
    private String roomId;
}

