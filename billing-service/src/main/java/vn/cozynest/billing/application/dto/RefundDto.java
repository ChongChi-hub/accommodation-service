package vn.cozynest.billing.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

public class RefundDto {
    public record CreateRefundCommand(
            String paymentIntentId,
            @NotBlank String contractId,
            @NotBlank String landlordId,
            @Min(1000) long amount,
            @NotBlank String bankCode,
            @NotBlank String accountNumber,
            @NotBlank String accountHolder,
            String expectedKycName,
            @NotBlank @Pattern(regexp = "^[0-9]{6}$") String otpCode,
            @NotBlank String reason
    ) {}

    public record RefundResponse(
            String id,
            String refundCode,
            String contractId,
            String tenantId,
            long amount,
            String bankCode,
            String accountNumber,
            String accountHolder,
            boolean otpVerified,
            boolean kycNameMatched,
            boolean requiresMakerChecker,
            String makerCheckerRequestId,
            String status,
            String reason,
            Instant createdAt
    ) {}
}

