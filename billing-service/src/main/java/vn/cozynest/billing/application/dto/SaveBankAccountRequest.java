package vn.cozynest.billing.application.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveBankAccountRequest(
        @NotBlank(message = "bankCode khong duoc de trong")
        String bankCode,
        @NotBlank(message = "bankName khong duoc de trong")
        String bankName,
        @NotBlank(message = "accountNumber khong duoc de trong")
        String accountNumber,
        @NotBlank(message = "accountHolder khong duoc de trong")
        String accountHolder,
        String qrCodeUrl
) {}
