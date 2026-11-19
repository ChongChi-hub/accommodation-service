package vn.cozynest.billing.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.cozynest.billing.application.exception.ObligationAlreadyPaidException;
import vn.cozynest.billing.application.exception.WebhookSecurityException;
import vn.cozynest.billing.application.port.BillingContractPort;
import vn.cozynest.billing.domain.repository.EscrowHoldRepository;
import vn.cozynest.billing.domain.repository.InvoiceRepository;

import java.time.LocalDate;

@Component
public class PaymentObligationValidator {
    private final InvoiceRepository invoiceRepository;
    private final EscrowHoldRepository escrowHoldRepository;
    private final BillingContractPort contractPort;

    @Autowired
    public PaymentObligationValidator(
            InvoiceRepository invoiceRepository,
            @Autowired(required = false) EscrowHoldRepository escrowHoldRepository,
            @Autowired(required = false) BillingContractPort contractPort) {
        this.invoiceRepository = invoiceRepository;
        this.escrowHoldRepository = escrowHoldRepository;
        this.contractPort = contractPort;
    }

    public long validateAndResolveDepositAmount(String contractId, String payerId, Long requestedAmount) {
        if (contractPort == null) {
            return requestedAmount != null ? requestedAmount : 0L;
        }
        var snap = contractPort.getContractSnapshot(contractId, LocalDate.now())
                .orElseThrow(() -> new WebhookSecurityException(404, "Khong tim thay hop dong hoac ban khong co quyen dat coc"));
        if (!payerId.equals(snap.tenantId())) {
            throw new WebhookSecurityException(404, "Khong tim thay hop dong hoac ban khong co quyen dat coc");
        }
        if (!"AWAITING_DEPOSIT".equals(snap.status())) {
            throw new ObligationAlreadyPaidException("Chi co the thanh toan coc khi hop dong o trang thai cho dat coc (hien tai: " + snap.status() + ")");
        }
        if (escrowHoldRepository != null) {
            escrowHoldRepository.findByContractId(contractId).ifPresent(h -> {
                if ("HELD".equals(h.getStatus()) || "RELEASED".equals(h.getStatus()) || "DISBURSED".equals(h.getStatus())) {
                    throw new ObligationAlreadyPaidException("Khoan tien coc cho hop dong nay da duoc bao dam tai Escrow");
                }
            });
        }
        long resolved = snap.depositAmount() > 0 ? snap.depositAmount() : snap.effectiveMonthlyRent();
        return resolved > 0 ? resolved : (requestedAmount != null ? requestedAmount : 0L);
    }

    public void validateInvoiceObligation(String invoiceId) {
        var inv = invoiceRepository.findById(invoiceId).orElse(null);
        if (inv != null && ("PAID".equals(inv.getStatus()) || "CANCELLED".equals(inv.getStatus()))) {
            throw new ObligationAlreadyPaidException("Hoa don da duoc thanh toan hoac da bi huy");
        }
    }

    public void validateLandlordPermission(String orderType, String orderId, String actorId) {
        if ("DEPOSIT".equalsIgnoreCase(orderType) && contractPort != null) {
            var snap = contractPort.getContractSnapshot(orderId, LocalDate.now())
                    .orElseThrow(() -> new WebhookSecurityException(404, "Khong tim thay hop dong lien ket"));
            if (!actorId.equals(snap.landlordId())) {
                throw new WebhookSecurityException(403, "Chi chu tro cua hop dong moi co quyen xac nhan nhan tien");
            }
        } else if ("INVOICE".equalsIgnoreCase(orderType)) {
            var inv = invoiceRepository.findById(orderId).orElse(null);
            if (inv == null || !actorId.equals(inv.getLandlordId())) {
                throw new WebhookSecurityException(403, "Chi chu tro cua hoa don moi co quyen xac nhan nhan tien");
            }
        }
    }

    public boolean isContractOwnedByLandlord(String orderType, String orderId, String landlordId) {
        if ("DEPOSIT".equalsIgnoreCase(orderType) && contractPort != null) {
            return contractPort.getContractSnapshot(orderId, LocalDate.now())
                    .map(c -> landlordId.equals(c.landlordId()))
                    .orElse(false);
        }
        if ("INVOICE".equalsIgnoreCase(orderType)) {
            return invoiceRepository.findById(orderId).map(i -> landlordId.equals(i.getLandlordId())).orElse(false);
        }
        return false;
    }
}
