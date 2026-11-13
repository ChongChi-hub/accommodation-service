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
        java.math.BigDecimal initialElectricityReading,
        java.math.BigDecimal initialWaterReading,
        java.math.BigDecimal electricityRate,
        java.math.BigDecimal waterRate,
        String physicalContractStatus,
        String physicalContractScanUrl,
        Instant physicalSignedAt,
        Instant tenantConfirmedAt,
        String tenantRejectReason,
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
                c.getInitialElectricityReading(),
                c.getInitialWaterReading(),
                c.getElectricityRate(),
                c.getWaterRate(),
                c.getPhysicalContractStatus(),
                c.getPhysicalContractScanUrl(),
                c.getPhysicalSignedAt(),
                c.getTenantConfirmedAt(),
                c.getTenantRejectReason(),
                c.getCreatedAt()
        );
    }
}

