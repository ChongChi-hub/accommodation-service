package vn.cozynest.booking.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UploadContractScanDto(
        @NotBlank(message = "scanUrl khong duoc de trong")
        String scanUrl
) {}

