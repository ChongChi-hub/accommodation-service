package vn.cozynest.booking.application.dto;

import vn.cozynest.booking.domain.model.Contract;

import java.time.Instant;
import java.time.LocalDate;

public record ContractDto(
        String id,
        String contractCode,
        String roomId,
        String tenantId,
        String landlordId,
        String requestId,
        LocalDate startDate,
        LocalDate endDate,
        long monthlyRent,
        long depositAmount,
        int paymentCycleMonths,
        int paymentDueDay,
        String terms,
        String documentHash,
        String pdfUrl,
        String status,
        Instant holdingExpiresAt,
        Instant tenantSignedAt,
        Instant landlordSignedAt,
        Instant createdAt
) {
    public static ContractDto from(Contract c) {
        if (c == null) return null;
        return new ContractDto(
                c.getId(),
                c.getContractCode(),
                c.getRoomId(),
                c.getTenantId(),
                c.getLandlordId(),
                c.getRequestId(),
                c.getStartDate(),
                c.getEndDate(),
                c.getMonthlyRent(),
                c.getDepositAmount(),
                c.getPaymentCycleMonths(),
                c.getPaymentDueDay(),
                c.getTerms(),
                c.getDocumentHash(),
                c.getPdfUrl(),
                c.getStatus(),
                c.getHoldingExpiresAt(),
                c.getTenantSignedAt(),
                c.getLandlordSignedAt(),
                c.getCreatedAt()
        );
    }
}

