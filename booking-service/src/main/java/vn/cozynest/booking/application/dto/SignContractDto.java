package vn.cozynest.booking.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignContractDto(
        @NotBlank(message = "Ma OTP khong duoc de trong")
        @Pattern(regexp = "^[0-9]{6}$", message = "Ma OTP phai co 6 chu so")
        String otpCode
) {}

