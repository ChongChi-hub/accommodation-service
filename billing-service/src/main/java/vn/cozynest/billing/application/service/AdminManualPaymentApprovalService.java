package vn.cozynest.billing.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.PaymentDto;
import vn.cozynest.billing.application.exception.WebhookSecurityException;
import vn.cozynest.billing.application.port.BillingIdentityVerificationPort;
import vn.cozynest.billing.application.port.BillingNotificationPort;
import vn.cozynest.billing.domain.model.BillingEventOutbox;
import vn.cozynest.billing.domain.model.PaymentIntent;
import vn.cozynest.billing.domain.model.PaymentProofRecord;
import vn.cozynest.billing.domain.repository.BillingEventOutboxRepository;
import vn.cozynest.billing.domain.repository.PaymentIntentRepository;
import vn.cozynest.billing.domain.repository.PaymentProofRegistryRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class AdminManualPaymentApprovalService {
    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentProofRegistryRepository proofRepository;
    private final BankInflowWebhookService webhookService;
    private final BillingNotificationPort notificationPort;
    private final BillingIdentityVerificationPort identityPort;
    private final BillingEventOutboxRepository outboxRepo;

    @Autowired
    public AdminManualPaymentApprovalService(
            PaymentIntentRepository paymentIntentRepository,
            @Autowired(required = false) PaymentProofRegistryRepository proofRepository,
            BankInflowWebhookService webhookService,
            @Autowired(required = false) BillingNotificationPort notificationPort,
            @Autowired(required = false) BillingIdentityVerificationPort identityPort,
            @Autowired(required = false) BillingEventOutboxRepository outboxRepo) {
        this.paymentIntentRepository = paymentIntentRepository;
        this.proofRepository = proofRepository;
        this.webhookService = webhookService;
        this.notificationPort = notificationPort;
        this.identityPort = identityPort;
        this.outboxRepo = outboxRepo;
    }

    @Transactional(readOnly = true)
    public List<PaymentDto.AdminApprovalItem> listPendingApprovals() {
        return paymentIntentRepository.findAll().stream()
                .filter(i -> "SUBMITTED".equals(i.getStatus())
                        || "PARTIAL_PAID".equals(i.getStatus())
                        || "OVERPAID".equals(i.getStatus())
                        || "MISMATCH".equals(i.getStatus()))
                .sorted(Comparator.comparing(PaymentIntent::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toApprovalItem)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentIntent approveByAdmin(String paymentCode, String rawBankTxCode, long actualAmount,
                                        String reason, String adminId, String checkerAdminId) {
        String effectiveTx = rawBankTxCode != null ? rawBankTxCode.trim() : "";
        if (effectiveTx.isBlank() && proofRepository != null) {
            effectiveTx = proofRepository.findLatestByPaymentCode(paymentCode)
                    .map(PaymentProofRecord::getNormalizedBankTxCode)
                    .orElse("");
        }
        String effectiveReason = (reason != null && reason.trim().length() >= 3)
                ? reason.trim()
                : "Admin đã kiểm tra tài khoản sàn và duyệt nhận đủ tiền";
        return webhookService.confirmManualByAdmin(paymentCode, effectiveTx, actualAmount, effectiveReason, adminId, checkerAdminId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentIntent rejectByAdmin(String paymentCode, String reason, String adminId) {
        PaymentIntent intent = paymentIntentRepository.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new WebhookSecurityException(404, "Không tìm thấy mã thanh toán: " + paymentCode));
        if ("PAID".equals(intent.getStatus()) || "OVERPAID".equals(intent.getStatus())) {
            throw new IllegalStateException("Giao dịch đã được duyệt trước đó, không thể hủy");
        }
        if ("CANCELLED".equals(intent.getStatus())) {
            return intent;
        }
        String safeReason = (reason != null && !reason.trim().isEmpty())
                ? reason.trim()
                : "Admin kiểm tra tài khoản sàn chưa nhận được tiền hợp lệ";
        intent.cancel();
        PaymentIntent saved = paymentIntentRepository.save(intent);
        if (outboxRepo != null) {
            String payload = String.format(
                    "{\"paymentCode\":\"%s\",\"orderId\":\"%s\",\"tenantId\":\"%s\",\"adminId\":\"%s\",\"reason\":\"%s\"}",
                    saved.getPaymentCode(), saved.getOrderId(), saved.getPayerId(), adminId, safeReason.replace("\"", ""));
            outboxRepo.save(BillingEventOutbox.create("PAYMENT_REJECTED_BY_ADMIN", "PAYMENT", saved.getId(), payload));
        }
        if (notificationPort != null) {
            notificationPort.notifyTenantDepositRejected(saved.getPayerId(), saved.getOrderId(), saved.getPaymentCode(), safeReason);
        }
        return saved;
    }

    private PaymentDto.AdminApprovalItem toApprovalItem(PaymentIntent intent) {
        Optional<PaymentProofRecord> proof = proofRepository != null
                ? proofRepository.findLatestByPaymentCode(intent.getPaymentCode())
                : Optional.empty();
        String tenantName = resolveTenantName(intent.getPayerId());
        return new PaymentDto.AdminApprovalItem(
                intent.getId(),
                intent.getPaymentCode(),
                intent.getPayerId(),
                tenantName,
                intent.getOrderType(),
                intent.getOrderId(),
                intent.getAmount(),
                intent.getPaidAmount(),
                intent.getStatus(),
                proof.map(PaymentProofRecord::getNormalizedBankTxCode).orElse(null),
                proof.isPresent(),
                intent.getExpiresAt(),
                intent.getCreatedAt()
        );
    }

    private String resolveTenantName(String payerId) {
        if (identityPort == null || payerId == null || payerId.isBlank()) return payerId;
        try {
            var kyc = identityPort.getKycStatus(payerId);
            if (kyc != null && kyc.verifiedFullName() != null && !kyc.verifiedFullName().isBlank()) {
                return kyc.verifiedFullName();
            }
        } catch (Exception ignored) {
        }
        return payerId;
    }
}
