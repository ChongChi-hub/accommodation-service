package vn.cozynest.booking.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AddOccupantDto(
        @NotBlank(message = "fullName khong duoc de trong")
        String fullName,

        @NotBlank(message = "phoneNumber khong duoc de trong")
        String phoneNumber,

        @NotBlank(message = "idCardNumber khong duoc de trong")
        String idCardNumber,

        String relationship,

        boolean primaryTenant
) {}

