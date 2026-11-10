package vn.cozynest.booking.infrastructure.persistence.mapper;

import vn.cozynest.booking.domain.model.*;
import vn.cozynest.booking.infrastructure.persistence.entity.*;

public final class ContractPersistenceMapper {

    private ContractPersistenceMapper() {}

    public static Contract toDomain(ContractJpaEntity entity) {
        if (entity == null) return null;
        return Contract.builder()
                .id(entity.getId())
                .contractCode(entity.getContractCode())
                .roomId(entity.getRoomId())
                .tenantId(entity.getTenantId())
                .landlordId(entity.getLandlordId())
                .requestId(entity.getRequestId())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .monthlyRent(entity.getMonthlyRent())
                .depositAmount(entity.getDepositAmount())
                .paymentCycleMonths(entity.getPaymentCycleMonths())
                .paymentDueDay(entity.getPaymentDueDay())
                .terms(entity.getTerms())
                .documentHash(entity.getDocumentHash())
                .pdfUrl(entity.getPdfUrl())
                .status(entity.getStatus())
                .holdingExpiresAt(entity.getHoldingExpiresAt())
                .tenantSignedAt(entity.getTenantSignedAt())
                .landlordSignedAt(entity.getLandlordSignedAt())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static ContractJpaEntity toEntity(Contract domain) {
        if (domain == null) return null;
        return ContractJpaEntity.builder()
                .id(domain.getId())
                .contractCode(domain.getContractCode())
                .roomId(domain.getRoomId())
                .tenantId(domain.getTenantId())
                .landlordId(domain.getLandlordId())
                .requestId(domain.getRequestId())
                .startDate(domain.getStartDate())
                .endDate(domain.getEndDate())
                .monthlyRent(domain.getMonthlyRent())
                .depositAmount(domain.getDepositAmount())
                .paymentCycleMonths(domain.getPaymentCycleMonths())
                .paymentDueDay(domain.getPaymentDueDay())
                .terms(domain.getTerms())
                .documentHash(domain.getDocumentHash())
                .pdfUrl(domain.getPdfUrl())
                .status(domain.getStatus())
                .holdingExpiresAt(domain.getHoldingExpiresAt())
                .tenantSignedAt(domain.getTenantSignedAt())
                .landlordSignedAt(domain.getLandlordSignedAt())
                .version(domain.getVersion())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public static RentalRequest toDomain(RentalRequestJpaEntity entity) {
        if (entity == null) return null;
        return RentalRequest.builder()
                .id(entity.getId())
                .roomId(entity.getRoomId())
                .tenantId(entity.getTenantId())
                .landlordId(entity.getLandlordId())
                .proposedMoveInDate(entity.getProposedMoveInDate())
                .numOccupants(entity.getNumOccupants())
                .message(entity.getMessage())
                .status(entity.getStatus())
                .contractId(entity.getContractId())
                .idempotencyKey(entity.getIdempotencyKey())
                .holdingExpiresAt(entity.getHoldingExpiresAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static RentalRequestJpaEntity toEntity(RentalRequest domain) {
        if (domain == null) return null;
        return RentalRequestJpaEntity.builder()
                .id(domain.getId())
                .roomId(domain.getRoomId())
                .tenantId(domain.getTenantId())
                .landlordId(domain.getLandlordId())
                .proposedMoveInDate(domain.getProposedMoveInDate())
                .numOccupants(domain.getNumOccupants())
                .message(domain.getMessage())
                .status(domain.getStatus())
                .contractId(domain.getContractId())
                .idempotencyKey(domain.getIdempotencyKey())
                .holdingExpiresAt(domain.getHoldingExpiresAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public static ContractAmendment toDomain(ContractAmendmentJpaEntity entity) {
        if (entity == null) return null;
        return ContractAmendment.builder()
                .id(entity.getId())
                .contractId(entity.getContractId())
                .amendmentCode(entity.getAmendmentCode())
                .amendmentType(entity.getAmendmentType())
                .newRent(entity.getNewRent())
                .newEndDate(entity.getNewEndDate())
                .effectiveDate(entity.getEffectiveDate())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .tenantSignedAt(entity.getTenantSignedAt())
                .landlordSignedAt(entity.getLandlordSignedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static ContractAmendmentJpaEntity toEntity(ContractAmendment domain) {
        if (domain == null) return null;
        return ContractAmendmentJpaEntity.builder()
                .id(domain.getId())
                .contractId(domain.getContractId())
                .amendmentCode(domain.getAmendmentCode())
                .amendmentType(domain.getAmendmentType())
                .newRent(domain.getNewRent())
                .newEndDate(domain.getNewEndDate())
                .effectiveDate(domain.getEffectiveDate())
                .reason(domain.getReason())
                .status(domain.getStatus())
                .tenantSignedAt(domain.getTenantSignedAt())
                .landlordSignedAt(domain.getLandlordSignedAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public static ContractOccupant toDomain(ContractOccupantJpaEntity entity) {
        if (entity == null) return null;
        return ContractOccupant.builder()
                .id(entity.getId())
                .contractId(entity.getContractId())
                .fullName(entity.getFullName())
                .phoneNumber(entity.getPhoneNumber())
                .idCardNumber(entity.getIdCardNumber())
                .relationship(entity.getRelationship())
                .primaryTenant(entity.isPrimaryTenant())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static ContractOccupantJpaEntity toEntity(ContractOccupant domain) {
        if (domain == null) return null;
        return ContractOccupantJpaEntity.builder()
                .id(domain.getId())
                .contractId(domain.getContractId())
                .fullName(domain.getFullName())
                .phoneNumber(domain.getPhoneNumber())
                .idCardNumber(domain.getIdCardNumber())
                .relationship(domain.getRelationship())
                .primaryTenant(domain.isPrimaryTenant())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public static SagaInstance toDomain(SagaInstanceJpaEntity entity) {
        if (entity == null) return null;
        return SagaInstance.builder()
                .id(entity.getId())
                .sagaId(entity.getSagaId())
                .sagaType(entity.getSagaType())
                .currentStep(entity.getCurrentStep())
                .payloadJson(entity.getPayloadJson())
                .errorMessage(entity.getErrorMessage())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static SagaInstanceJpaEntity toEntity(SagaInstance domain) {
        if (domain == null) return null;
        return SagaInstanceJpaEntity.builder()
                .id(domain.getId())
                .sagaId(domain.getSagaId())
                .sagaType(domain.getSagaType())
                .currentStep(domain.getCurrentStep())
                .payloadJson(domain.getPayloadJson())
                .errorMessage(domain.getErrorMessage())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

