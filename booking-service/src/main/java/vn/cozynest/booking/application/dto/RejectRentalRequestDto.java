package vn.cozynest.booking.application.dto;

import jakarta.validation.constraints.Size;

public record RejectRentalRequestDto(
        @Size(max = 500, message = "Ly do tu choi toi da 500 ky tu")
        String reason
) {}

