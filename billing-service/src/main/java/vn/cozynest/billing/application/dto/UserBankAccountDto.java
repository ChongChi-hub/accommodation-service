package vn.cozynest.billing.application.dto;

import java.time.Instant;

public record UserBankAccountDto(
        String id,
        String userId,
        String bankCode,
        String bankName,
        String accountNumber,
        String accountHolder,
        String qrCodeUrl,
        Instant createdAt,
        Instant updatedAt
) {}
