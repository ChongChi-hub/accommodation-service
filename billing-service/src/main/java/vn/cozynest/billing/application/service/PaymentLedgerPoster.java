package vn.cozynest.billing.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.application.port.BillingContractPort;
import vn.cozynest.billing.domain.model.*;
import vn.cozynest.billing.domain.repository.InvoiceRepository;

import java.time.LocalDate;
import java.util.List;

@Component
public class PaymentLedgerPoster {
    private final LedgerService ledgerService;
    private final InvoiceRepository invoiceRepository;
    private final EscrowService escrowService;
    private final BillingContractPort contractPort;
    private final String fintechMode;
    private final boolean liveCustodyEnabled;

    @Autowired
    public PaymentLedgerPoster(
            LedgerService ledgerService,
            InvoiceRepository invoiceRepository,
            @Autowired(required = false) EscrowService escrowService,
            @Autowired(required = false) BillingContractPort contractPort,
            @Value("${fintech.mode:SANDBOX_ONLY}") String fintechMode,
            @Value("${fintech.escrow.live-custody-enabled:false}") boolean liveCustodyEnabled) {
        this.ledgerService = ledgerService;
        this.invoiceRepository = invoiceRepository;
        this.escrowService = escrowService;
        this.contractPort = contractPort;
        this.fintechMode = fintechMode;
        this.liveCustodyEnabled = liveCustodyEnabled;
    }

    public void postLedgerForPayment(PaymentIntent intent, long paidChunk, String txRef) {
        if ("LIVE".equalsIgnoreCase(fintechMode) && !liveCustodyEnabled && "DEPOSIT".equalsIgnoreCase(intent.getOrderType())) {
            throw new IllegalStateException("BLOCKED_HUMAN_REVIEW [LEGAL-02]: Live custody disabled under Model A.");
        }
        ledgerService.getOrCreateAccount(LedgerService.ACC_GATEWAY, "Tài khoản tiền về cổng thanh toán", AccountType.ASSET, null);
        ledgerService.getOrCreateAccount(LedgerService.ACC_ESCROW, "Tài khoản cọc ký quỹ Escrow", AccountType.LIABILITY, null);
        ledgerService.getOrCreateAccount(LedgerService.ACC_REVENUE, "Doanh thu phí dịch vụ sàn Tổ Ấm", AccountType.REVENUE, null);

        String refKey = txRef != null && !txRef.isBlank() ? txRef : ("PAYWH-" + intent.getPaymentCode() + "-" + intent.getPaidAmount());
        if ("DEPOSIT".equalsIgnoreCase(intent.getOrderType())) {
            postDeposit(intent, paidChunk, refKey);
        } else if ("INVOICE".equalsIgnoreCase(intent.getOrderType())) {
            postInvoice(intent, paidChunk, refKey);
        }
    }

    private void postDeposit(PaymentIntent intent, long paidChunk, String refKey) {
        ledgerService.postEntryIdempotent(refKey, "Thu tiền cọc hợp đồng " + intent.getOrderId(),
                "DEPOSIT", intent.getOrderId(), "SYSTEM_WEBHOOK",
                List.of(
                        new LedgerDto.PostingItem(LedgerService.ACC_GATEWAY, PostingDirection.DEBIT, paidChunk),
                        new LedgerDto.PostingItem(LedgerService.ACC_ESCROW, PostingDirection.CREDIT, paidChunk)));

        if (escrowService != null) {
            String contractId = intent.getOrderId();
            String tenantId = intent.getPayerId();
            String landlordId = "u-landlord-01";
            if (contractPort != null) {
                var snap = contractPort.getContractSnapshot(contractId, LocalDate.now());
                if (snap.isPresent()) {
                    tenantId = snap.get().tenantId();
                    landlordId = snap.get().landlordId();
                }
            }
            escrowService.recordDepositPaid(contractId, tenantId, landlordId, paidChunk, 72);
        }
        if (contractPort != null && ("PAID".equals(intent.getStatus()) || "OVERPAID".equals(intent.getStatus()))) {
            contractPort.activateContractAfterDeposit(intent.getOrderId(), intent.getPaymentCode(), intent.getPaidAmount());
        }
    }

    private void postInvoice(PaymentIntent intent, long paidChunk, String refKey) {
        Invoice invoice = invoiceRepository.findById(intent.getOrderId()).orElse(null);
        String landlordId = invoice != null ? invoice.getLandlordId() : "landlord-system";
        ledgerService.getOrCreateWallet(landlordId, "Chủ trọ");
        long baseRent = invoice != null ? invoice.getBaseRent() : 0L;
        long platformFee = (baseRent > 0 && paidChunk >= baseRent) ? (baseRent * 5 / 100) : (paidChunk * 5 / 100);
        long landlordAmount = paidChunk - platformFee;
        ledgerService.postEntryIdempotent(refKey, "Thanh toán hóa đơn phòng " + intent.getOrderId(),
                "INVOICE_PAYMENT", intent.getOrderId(), "SYSTEM_WEBHOOK",
                List.of(
                        new LedgerDto.PostingItem(LedgerService.ACC_GATEWAY, PostingDirection.DEBIT, paidChunk),
                        new LedgerDto.PostingItem(ledgerService.getWalletAccountNumber(landlordId), PostingDirection.CREDIT, landlordAmount),
                        new LedgerDto.PostingItem(LedgerService.ACC_REVENUE, PostingDirection.CREDIT, platformFee)));
        if (invoice != null) {
            invoice.recordPayment(paidChunk);
            invoiceRepository.save(invoice);
        }
    }
}
