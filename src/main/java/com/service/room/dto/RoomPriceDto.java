package com.service.room.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomPriceDto {
    private String id;
    private String label;
    private BigDecimal price;
    private String unit;
    private Boolean isMetered;
    private String meterType;
}

