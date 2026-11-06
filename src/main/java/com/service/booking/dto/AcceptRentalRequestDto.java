package com.service.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptRentalRequestDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private String terms;
}

