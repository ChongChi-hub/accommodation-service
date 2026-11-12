package vn.cozynest.billing.application.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.InvoiceDto;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.domain.model.Invoice;
import vn.cozynest.billing.domain.model.PostingDirection;
import vn.cozynest.billing.domain.repository.InvoiceRepository;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class InvoiceService {
    private final InvoiceRepository repository;
    private final LedgerService ledgerService;

    public InvoiceService(InvoiceRepository repository, LedgerService ledgerService) {
        this.repository = repository;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public InvoiceDto.InvoiceResponse generateInvoice(InvoiceDto.GenerateInvoiceCommand cmd) {
        List<Invoice> existing = repository.findByContractIdAndPeriodMonth(cmd.contractId(), cmd.periodMonth());
        Invoice activeStandard = existing.stream()
                .filter(i -> "STANDARD".equals(i.getInvoiceType()) && !"CANCELLED".equals(i.getStatus()))
                .findFirst().orElse(null);
        if (activeStandard != null) {
            return toResponse(activeStandard);
        }

        String code = "INV-" + cmd.periodMonth().getYear()
                + String.format("%02d", cmd.periodMonth().getMonthValue())
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Invoice invoice = Invoice.create(
                code, cmd.contractId(), cmd.utilityReadingId(), cmd.tenantId(), cmd.landlordId(),
                cmd.periodMonth(), cmd.baseRent(), cmd.electricityAmount(), cmd.waterAmount(),
                cmd.serviceAmount(), cmd.otherAmount(), cmd.dueDate()
        );
        return toResponse(repository.save(invoice));
    }

    @Transactional
    public List<InvoiceDto.InvoiceResponse> generateBatch(List<InvoiceDto.GenerateInvoiceCommand> items, String landlordId, boolean isAdmin) {
        List<InvoiceDto.InvoiceResponse> results = new ArrayList<>();
        for (InvoiceDto.GenerateInvoiceCommand item : items) {
            if (!isAdmin && !item.landlordId().equals(landlordId)) {
                throw new AccessDeniedException("Khong co quyen phat hanh hoa don cho chu tro khac");
            }
            results.add(generateInvoice(item));
        }
        return results;
    }

    @Transactional
    public InvoiceDto.InvoiceResponse issueCreditNote(String invoiceId, InvoiceDto.CreditNoteCommand cmd, String actorId, boolean isAdmin) {
        Invoice target = findOrThrow(invoiceId);
        if (!isAdmin && !target.getLandlordId().equals(actorId)) {
            throw new AccessDeniedException("Ban khong co quyen lap phieu dieu chinh giam cho hoa don nay");
        }
        String cnCode = "CN-" + target.getInvoiceCode() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        Invoice creditNote = Invoice.createCreditNote(target, cnCode, cmd.amount(), cmd.reason());
        Invoice saved = repository.save(creditNote);
        if (ledgerService != null && target.getPaidAmount() > 0) {
            String walletAcc = ledgerService.getWalletAccountNumber(target.getLandlordId());
            ledgerService.postEntryIdempotent(
                    "CN-" + saved.getId(),
                    "Dieu chinh giam hoa don " + target.getInvoiceCode() + ": " + cmd.reason(),
                    "CREDIT_NOTE", saved.getId(), actorId,
                    List.of(
                            new LedgerDto.PostingItem(walletAcc, PostingDirection.DEBIT, cmd.amount()),
                            new LedgerDto.PostingItem(LedgerService.ACC_ESCROW, PostingDirection.CREDIT, cmd.amount())
                    )
            );
        }
        return toResponse(saved);
    }

    @Transactional
    public InvoiceDto.InvoiceResponse recordPayment(String invoiceId, long amount, String actorId, boolean isAdmin) {
        Invoice invoice = findOrThrow(invoiceId);
        if (!isAdmin && !invoice.getTenantId().equals(actorId) && !invoice.getLandlordId().equals(actorId)) {
            throw new AccessDeniedException("Khong co quyen thanh toan hoa don nay");
        }
        invoice.recordPayment(amount);
        return toResponse(repository.save(invoice));
    }

    @Scheduled(cron = "0 0 1 * * *", zone = "UTC")
    @Transactional
    public int markOverdueInvoices() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<Invoice> candidates = repository.findOverdueCandidates(today);
        for (Invoice inv : candidates) {
            inv.markOverdue(today);
            repository.save(inv);
        }
        return candidates.size();
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto.InvoiceResponse> getInvoicesForTenant(String tenantId) {
        return repository.findByTenantId(tenantId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto.InvoiceResponse> getInvoicesForLandlord(String landlordId) {
        return repository.findByLandlordId(landlordId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public InvoiceDto.InvoiceResponse getInvoiceById(String id, String actorId, boolean isAdmin) {
        Invoice inv = findOrThrow(id);
        if (!isAdmin && !inv.getTenantId().equals(actorId) && !inv.getLandlordId().equals(actorId)) {
            throw new AccessDeniedException("Khong co quyen xem hoa don nay");
        }
        return toResponse(inv);
    }

    @Transactional
    public void cancelInvoice(String id, String actorId, boolean isAdmin) {
        Invoice inv = findOrThrow(id);
        if (!isAdmin && !inv.getLandlordId().equals(actorId)) {
            throw new AccessDeniedException("Ban khong co quyen huy hoa don nay");
        }
        inv.cancel();
        repository.save(inv);
    }

    private Invoice findOrThrow(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn: " + id));
    }

    private InvoiceDto.InvoiceResponse toResponse(Invoice i) {
        return new InvoiceDto.InvoiceResponse(
                i.getId(), i.getInvoiceCode(), i.getContractId(), i.getUtilityReadingId(),
                i.getTenantId(), i.getLandlordId(), i.getPeriodMonth(),
                i.getBaseRent(), i.getElectricityAmount(), i.getWaterAmount(),
                i.getServiceAmount(), i.getOtherAmount(), i.getTotalAmount(),
                i.getPaidAmount(), i.getDueDate(), i.getStatus(),
                i.getInvoiceType(), i.getReferenceInvoiceId(), i.getCreditNoteReason(), i.getCreatedAt()
        );
    }
}
