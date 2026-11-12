package vn.cozynest.billing.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.billing.domain.model.RefundRequest;
import vn.cozynest.billing.domain.repository.RefundRequestRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.RefundRequestJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataRefundRequestRepository;

import java.util.List;
import java.util.Optional;

@Component
public class RefundRequestRepositoryAdapter implements RefundRequestRepository {
    private final SpringDataRefundRequestRepository repository;

    public RefundRequestRepositoryAdapter(SpringDataRefundRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public RefundRequest save(RefundRequest r) {
        RefundRequestJpaEntity saved = repository.save(RefundRequestJpaEntity.builder()
                .id(r.getId()).refundCode(r.getRefundCode()).paymentIntentId(r.getPaymentIntentId())
                .contractId(r.getContractId()).tenantId(r.getTenantId()).landlordId(r.getLandlordId())
                .amount(r.getAmount()).bankCode(r.getBankCode()).accountNumber(r.getAccountNumber())
                .accountHolder(r.getAccountHolder()).otpVerified(r.isOtpVerified())
                .kycNameMatched(r.isKycNameMatched()).requiresMakerChecker(r.isRequiresMakerChecker())
                .makerCheckerRequestId(r.getMakerCheckerRequestId()).status(r.getStatus())
                .reason(r.getReason()).failureReason(r.getFailureReason()).retryCount(r.getRetryCount())
                .createdAt(r.getCreatedAt()).updatedAt(r.getUpdatedAt())
                .build());
        return toDomain(saved);
    }

    @Override
    public Optional<RefundRequest> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<RefundRequest> findByTenantId(String tenantId) {
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<RefundRequest> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<RefundRequest> findByPaymentIntentId(String paymentIntentId) {
        return repository.findByPaymentIntentId(paymentIntentId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<RefundRequest> findByStatus(String status) {
        return repository.findByStatusOrderByUpdatedAtDesc(status).stream().map(this::toDomain).toList();
    }

    private RefundRequest toDomain(RefundRequestJpaEntity e) {
        return RefundRequest.builder()
                .id(e.getId()).refundCode(e.getRefundCode()).paymentIntentId(e.getPaymentIntentId())
                .contractId(e.getContractId()).tenantId(e.getTenantId()).landlordId(e.getLandlordId())
                .amount(e.getAmount()).bankCode(e.getBankCode()).accountNumber(e.getAccountNumber())
                .accountHolder(e.getAccountHolder()).otpVerified(e.isOtpVerified())
                .kycNameMatched(e.isKycNameMatched()).requiresMakerChecker(e.isRequiresMakerChecker())
                .makerCheckerRequestId(e.getMakerCheckerRequestId()).status(e.getStatus())
                .reason(e.getReason()).failureReason(e.getFailureReason()).retryCount(e.getRetryCount())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }
}

