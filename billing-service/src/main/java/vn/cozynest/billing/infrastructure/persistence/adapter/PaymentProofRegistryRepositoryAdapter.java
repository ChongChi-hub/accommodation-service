package vn.cozynest.billing.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.billing.domain.model.PaymentProofAccessLog;
import vn.cozynest.billing.domain.model.PaymentProofRecord;
import vn.cozynest.billing.domain.model.PaymentReferenceCodeCodec;
import vn.cozynest.billing.domain.repository.PaymentProofRegistryRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.PaymentProofAccessLogJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.entity.PaymentProofRecordJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataPaymentProofAccessLogRepository;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataPaymentProofRecordRepository;

import java.util.List;
import java.util.Optional;

@Component
public class PaymentProofRegistryRepositoryAdapter implements PaymentProofRegistryRepository {
    private final SpringDataPaymentProofRecordRepository recordRepo;
    private final SpringDataPaymentProofAccessLogRepository logRepo;

    public PaymentProofRegistryRepositoryAdapter(SpringDataPaymentProofRecordRepository recordRepo,
                                                 SpringDataPaymentProofAccessLogRepository logRepo) {
        this.recordRepo = recordRepo;
        this.logRepo = logRepo;
    }

    @Override
    public boolean existsByNormalizedBankTxCode(String normalizedBankTxCode) {
        String norm = PaymentReferenceCodeCodec.normalizeAlphanumericUpper(normalizedBankTxCode);
        return !norm.isEmpty() && recordRepo.existsByNormalizedBankTxCode(norm);
    }

    @Override
    public boolean existsBySha256Hash(String sha256Hash) {
        return sha256Hash != null && !sha256Hash.isBlank() && recordRepo.existsBySha256Hash(sha256Hash.trim().toLowerCase());
    }

    @Override
    public Optional<PaymentProofRecord> findById(String id) {
        return recordRepo.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<PaymentProofRecord> findLatestByPaymentCode(String paymentCode) {
        return recordRepo.findFirstByPaymentCodeOrderByCreatedAtDesc(paymentCode).map(this::toDomain);
    }

    @Override
    public List<PaymentProofRecord> findByOrderId(String orderId) {
        return recordRepo.findByOrderIdOrderByCreatedAtDesc(orderId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<PaymentProofRecord> findAll() {
        return recordRepo.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public PaymentProofRecord save(PaymentProofRecord d) {
        PaymentProofRecordJpaEntity e = PaymentProofRecordJpaEntity.builder()
                .id(d.getId())
                .paymentCode(d.getPaymentCode())
                .orderType(d.getOrderType())
                .orderId(d.getOrderId())
                .uploaderId(d.getUploaderId())
                .normalizedBankTxCode(d.getNormalizedBankTxCode())
                .sha256Hash(d.getSha256Hash())
                .dhash(d.getDhash())
                .nearDuplicateFlag(d.isNearDuplicateFlag())
                .nearDuplicateOfId(d.getNearDuplicateOfId())
                .mimeType(d.getMimeType())
                .storageKey(d.getStorageKey())
                .createdAt(d.getCreatedAt())
                .build();
        return toDomain(recordRepo.save(e));
    }

    @Override
    public PaymentProofAccessLog saveAccessLog(PaymentProofAccessLog d) {
        PaymentProofAccessLogJpaEntity e = PaymentProofAccessLogJpaEntity.builder()
                .id(d.getId())
                .proofId(d.getProofId())
                .paymentCode(d.getPaymentCode())
                .actorId(d.getActorId())
                .actorRole(d.getActorRole())
                .action(d.getAction())
                .ipAddress(d.getIpAddress())
                .createdAt(d.getCreatedAt())
                .build();
        PaymentProofAccessLogJpaEntity saved = logRepo.save(e);
        return new PaymentProofAccessLog(saved.getId(), saved.getProofId(), saved.getPaymentCode(),
                saved.getActorId(), saved.getActorRole(), saved.getAction(), saved.getIpAddress(), saved.getCreatedAt());
    }

    @Override
    public List<PaymentProofAccessLog> findAccessLogsByProofId(String proofId) {
        return logRepo.findByProofIdOrderByCreatedAtDesc(proofId).stream()
                .map(e -> new PaymentProofAccessLog(e.getId(), e.getProofId(), e.getPaymentCode(),
                        e.getActorId(), e.getActorRole(), e.getAction(), e.getIpAddress(), e.getCreatedAt()))
                .toList();
    }

    private PaymentProofRecord toDomain(PaymentProofRecordJpaEntity e) {
        return new PaymentProofRecord(
                e.getId(), e.getPaymentCode(), e.getOrderType(), e.getOrderId(),
                e.getUploaderId(), e.getNormalizedBankTxCode(), e.getSha256Hash(),
                e.getDhash(), e.isNearDuplicateFlag(), e.getNearDuplicateOfId(),
                e.getMimeType(), e.getStorageKey(), e.getCreatedAt()
        );
    }
}

