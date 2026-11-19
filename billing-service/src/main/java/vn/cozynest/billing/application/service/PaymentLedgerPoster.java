package vn.cozynest.billing.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.application.port.BillingContractPort;
import vn.cozynest.billing.application.port.BillingNotificationPort;
import vn.cozynest.billing.domain.model.*;
import vn.cozynest.billing.domain.repository.InvoiceRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Component
public class PaymentLedgerPoster {
    private final LedgerService ledgerService;
    private final InvoiceRepository invoiceRepository;
    private final EscrowService escrowService;
    private final BillingContractPort contractPort;
    private final BillingNotificationPort notificationPort;
    private final String fintechMode;
    private final boolean liveCustodyEnabled;
    private final int commissionPercent;

    public PaymentLedgerPoster(LedgerService ledgerService, InvoiceRepository invoiceRepository,
                               EscrowService escrowService, BillingContractPort contractPort,
                               String fintechMode, boolean liveCustodyEnabled) {
        this(ledgerService, invoiceRepository, escrowService, contractPort, null, fintechMode, liveCustodyEnabled, 5);
    }

    public PaymentLedgerPoster(LedgerService ledgerService, InvoiceRepository invoiceRepository,
                               EscrowService escrowService, BillingContractPort contractPort,
                               BillingNotificationPort notificationPort, String fintechMode, boolean liveCustodyEnabled) {
        this(ledgerService, invoiceRepository, escrowService, contractPort, notificationPort, fintechMode, liveCustodyEnabled, 5);
    }

    @Autowired
    public PaymentLedgerPoster(
            LedgerService ledgerService,
            InvoiceRepository invoiceRepository,
            @Autowired(required = false) EscrowService escrowService,
            @Autowired(required = false) BillingContractPort contractPort,
            @Autowired(required = false) BillingNotificationPort notificationPort,
            @Value("${fintech.mode:SANDBOX_ONLY}") String fintechMode,
            @Value("${fintech.escrow.live-custody-enabled:false}") boolean liveCustodyEnabled,
            @Value("${fintech.commission.percent:${commission.percent:5}}") int commissionPercent) {
        this.ledgerService = ledgerService;
        this.invoiceRepository = invoiceRepository;
        this.escrowService = escrowService;
        this.contractPort = contractPort;
        this.notificationPort = notificationPort;
        this.fintechMode = fintechMode;
        this.liveCustodyEnabled = liveCustodyEnabled;
        this.commissionPercent = Math.max(0, Math.min(100, commissionPercent));
    }

    public static long computeCommissionHalfUp(long totalAmount, int commissionPercent) {
        if (totalAmount <= 0 || commissionPercent <= 0) return 0L;
        return BigDecimal.valueOf(totalAmount)
                .multiply(BigDecimal.valueOf(commissionPercent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .longValue();
    }

    public void postLedgerForPayment(PaymentIntent intent, long paidChunk, String txRef) {
        postLedgerForPaymentWithProof(intent, paidChunk, txRef, null, null);
    }

    public void postLedgerForPaymentWithProof(PaymentIntent intent, long paidChunk, String txRef, String rawNote, String receiptUrl) {
        if ("LIVE".equalsIgnoreCase(fintechMode) && !liveCustodyEnabled && "DEPOSIT".equalsIgnoreCase(intent.getOrderType())) {
            throw new IllegalStateException("BLOCKED_HUMAN_REVIEW [LEGAL-02]: Live custody disabled under Model A.");
        }
        ledgerService.getOrCreateAccount(LedgerService.ACC_GATEWAY, "Tài khoản tiền về cổng thanh toán", AccountType.ASSET, null);
        ledgerService.getOrCreateAccount(LedgerService.ACC_ESCROW, "Tài khoản cọc ký quỹ Escrow", AccountType.LIABILITY, null);
        ledgerService.getOrCreateAccount(LedgerService.ACC_PLATFORM_REVENUE, "Doanh thu phí dịch vụ sàn Tổ Ấm", AccountType.REVENUE, null);

        String bankTxCode = extractBankTxCode(rawNote);
        String receiptHash = extractOrComputeReceiptHash(rawNote, receiptUrl);
        String refKey = bankTxCode != null ? ("BANKTX-" + bankTxCode)
                : (txRef != null && !txRef.isBlank() ? txRef : ("PAYWH-" + intent.getPaymentCode() + "-" + intent.getPaidAmount()));

        if ("DEPOSIT".equalsIgnoreCase(intent.getOrderType())) {
            postDeposit(intent, paidChunk, refKey, bankTxCode, receiptHash, receiptUrl);
        } else if ("INVOICE".equalsIgnoreCase(intent.getOrderType())) {
            postInvoice(intent, paidChunk, refKey);
        }
    }

    private void postDeposit(PaymentIntent intent, long paidChunk, String refKey,
                             String bankTxCode, String receiptHash, String receiptUrl) {
        if (intent == null || paidChunk <= 0) {
            throw new IllegalStateException("Chưa xác thực đủ tiền cọc tại tài khoản Sàn");
        }
        String contractId = intent.getOrderId();
        if ("PARTIAL_PAID".equals(intent.getStatus())) {
            ledgerService.postEntryIdempotent(refKey, "Thu một phần tiền cọc thiếu hợp đồng " + contractId,
                    "DEPOSIT_PARTIAL", contractId, "SYSTEM_WEBHOOK",
                    List.of(
                            new LedgerDto.PostingItem(LedgerService.ACC_GATEWAY, PostingDirection.DEBIT, paidChunk),
                            new LedgerDto.PostingItem(LedgerService.ACC_ESCROW, PostingDirection.CREDIT, paidChunk)));
            return;
        }
        if (paidChunk < intent.getAmount() || (!"PAID".equals(intent.getStatus()) && !"OVERPAID".equals(intent.getStatus()))) {
            throw new IllegalStateException("Chưa xác thực đủ tiền cọc tại tài khoản Sàn, từ chối ghi nhận Escrow và thông báo chủ trọ");
        }
        String tenantId = intent.getPayerId();
        String landlordId = "u-landlord-01";
        if (contractPort != null) {
            var snap = contractPort.getContractSnapshot(contractId, LocalDate.now());
            if (snap.isPresent()) {
                if ("CANCELLED".equalsIgnoreCase(snap.get().status()) || "TERMINATED".equalsIgnoreCase(snap.get().status())) {
                    throw new IllegalStateException("Hợp đồng đã hủy hoặc chấm dứt, không thể ghi nhận tiền cọc");
                }
                tenantId = snap.get().tenantId();
                landlordId = snap.get().landlordId();
            }
        }
        if (escrowService != null) {
            EscrowHold hold = escrowService.recordDepositPaidWithProof(
                    contractId, tenantId, landlordId, paidChunk, 72,
                    intent.getPaymentCode(), bankTxCode, receiptHash, receiptUrl);
            if (hold == null || !"HELD".equals(hold.getStatus())) {
                throw new IllegalStateException("Không thể kích hoạt quỹ ký quỹ Escrow cho hợp đồng " + contractId);
            }
        }
        ledgerService.postEntryIdempotent(refKey, "Thu tiền cọc hợp đồng " + contractId + " vào tài khoản Sàn",
                "DEPOSIT", contractId, "ESCROW_PLATFORM",
                List.of(
                        new LedgerDto.PostingItem(LedgerService.ACC_GATEWAY, PostingDirection.DEBIT, paidChunk),
                        new LedgerDto.PostingItem(LedgerService.ACC_ESCROW, PostingDirection.CREDIT, paidChunk)));

        if (contractPort != null) {
            contractPort.activateContractAfterDeposit(contractId, intent.getPaymentCode(), intent.getPaidAmount());
        }
        final String targetLandlord = landlordId;
        final String targetTenant = tenantId;
        if (notificationPort != null) {
            runAfterCommit(() -> {
                notificationPort.notifyDepositConfirmed(targetLandlord, contractId, paidChunk);
                notificationPort.notifyTenantDepositConfirmed(targetTenant, contractId, paidChunk);
            });
        }
    }

    private void postInvoice(PaymentIntent intent, long paidChunk, String refKey) {
        Invoice invoice = invoiceRepository.findById(intent.getOrderId()).orElse(null);
        String landlordId = invoice != null ? invoice.getLandlordId() : "landlord-system";
        ledgerService.getOrCreateWallet(landlordId, "Chủ trọ");
        long platformFee = computeCommissionHalfUp(paidChunk, commissionPercent);
        long landlordAmount = paidChunk - platformFee;
        ledgerService.postEntryIdempotent(refKey, "Thanh toán hóa đơn phòng " + intent.getOrderId() + " (Hoa hồng " + commissionPercent + "%)",
                "INVOICE_PAYMENT", intent.getOrderId(), "SYSTEM_WEBHOOK",
                List.of(
                        new LedgerDto.PostingItem(LedgerService.ACC_GATEWAY, PostingDirection.DEBIT, paidChunk),
                        new LedgerDto.PostingItem(ledgerService.getLandlordAvailableAccountNumber(landlordId), PostingDirection.CREDIT, landlordAmount),
                        new LedgerDto.PostingItem(LedgerService.ACC_PLATFORM_REVENUE, PostingDirection.CREDIT, platformFee)));
        if (invoice != null) {
            invoice.recordPayment(paidChunk);
            invoiceRepository.save(invoice);
            if (notificationPort != null) {
                final String targetLandlord = landlordId;
                final String targetTenant = invoice.getTenantId();
                final String invCode = invoice.getInvoiceCode();
                runAfterCommit(() -> notificationPort.notifyInvoicePaidWithCommission(
                        targetLandlord, targetTenant, invCode, paidChunk, platformFee, landlordAmount));
            }
        }
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private String extractBankTxCode(String rawNote) {
        if (rawNote == null || rawNote.isBlank()) return null;
        for (String part : rawNote.trim().split("\\|")) {
            if (part.startsWith("BANK_TX:")) {
                String code = PaymentReferenceCodeCodec.normalizeAlphanumericUpper(part.substring("BANK_TX:".length()));
                return code.isEmpty() ? null : code;
            }
        }
        String norm = PaymentReferenceCodeCodec.normalizeAlphanumericUpper(rawNote);
        return norm.length() >= 6 && norm.length() <= 36 ? norm : null;
    }

    private String extractOrComputeReceiptHash(String rawNote, String receiptUrl) {
        if (rawNote != null) {
            for (String part : rawNote.split("\\|")) {
                if (part.startsWith("HASH:")) {
                    String h = part.substring("HASH:".length()).trim().toLowerCase();
                    if (!h.isEmpty()) return h;
                }
            }
        }
        return null;
    }
}
