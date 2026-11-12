package vn.cozynest.billing.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.billing.domain.model.Invoice;
import vn.cozynest.billing.domain.repository.InvoiceRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.InvoiceJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataInvoiceRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class InvoiceRepositoryAdapter implements InvoiceRepository {
    private final SpringDataInvoiceRepository repository;

    public InvoiceRepositoryAdapter(SpringDataInvoiceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Invoice save(Invoice domain) {
        return toDomain(repository.save(toEntity(domain)));
    }

    @Override
    public Optional<Invoice> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Invoice> findByInvoiceCode(String invoiceCode) {
        return repository.findByInvoiceCode(invoiceCode).map(this::toDomain);
    }

    @Override
    public List<Invoice> findByTenantId(String tenantId) {
        return repository.findByTenantIdOrderByPeriodMonthDesc(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Invoice> findByLandlordId(String landlordId) {
        return repository.findByLandlordIdOrderByPeriodMonthDesc(landlordId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Invoice> findByContractId(String contractId) {
        return repository.findByContractIdOrderByPeriodMonthDesc(contractId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Invoice> findByUtilityReadingId(String utilityReadingId) {
        return repository.findByUtilityReadingId(utilityReadingId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Invoice> findByContractIdAndPeriodMonth(String contractId, LocalDate periodMonth) {
        return repository.findByContractIdAndPeriodMonth(contractId, periodMonth).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Invoice> findOverdueCandidates(LocalDate today) {
        return repository.findOverdueCandidates(today).stream().map(this::toDomain).toList();
    }

    private Invoice toDomain(InvoiceJpaEntity e) {
        return Invoice.builder()
                .id(e.getId()).invoiceCode(e.getInvoiceCode()).contractId(e.getContractId())
                .utilityReadingId(e.getUtilityReadingId()).tenantId(e.getTenantId()).landlordId(e.getLandlordId())
                .periodMonth(e.getPeriodMonth()).baseRent(e.getBaseRent()).electricityAmount(e.getElectricityAmount())
                .waterAmount(e.getWaterAmount()).serviceAmount(e.getServiceAmount()).otherAmount(e.getOtherAmount())
                .lateFeeAmount(e.getLateFeeAmount()).creditAppliedAmount(e.getCreditAppliedAmount())
                .totalAmount(e.getTotalAmount()).paidAmount(e.getPaidAmount())
                .overpaymentCreditAmount(e.getOverpaymentCreditAmount()).dueDate(e.getDueDate())
                .status(e.getStatus()).invoiceType(e.getInvoiceType()).referenceInvoiceId(e.getReferenceInvoiceId())
                .creditNoteReason(e.getCreditNoteReason()).overdueNotifiedAt(e.getOverdueNotifiedAt())
                .reminderCount(e.getReminderCount()).version(e.getVersion())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    private InvoiceJpaEntity toEntity(Invoice d) {
        return InvoiceJpaEntity.builder()
                .id(d.getId()).invoiceCode(d.getInvoiceCode()).contractId(d.getContractId())
                .utilityReadingId(d.getUtilityReadingId()).tenantId(d.getTenantId()).landlordId(d.getLandlordId())
                .periodMonth(d.getPeriodMonth()).baseRent(d.getBaseRent()).electricityAmount(d.getElectricityAmount())
                .waterAmount(d.getWaterAmount()).serviceAmount(d.getServiceAmount()).otherAmount(d.getOtherAmount())
                .lateFeeAmount(d.getLateFeeAmount()).creditAppliedAmount(d.getCreditAppliedAmount())
                .totalAmount(d.getTotalAmount()).paidAmount(d.getPaidAmount())
                .overpaymentCreditAmount(d.getOverpaymentCreditAmount()).dueDate(d.getDueDate())
                .status(d.getStatus()).invoiceType(d.getInvoiceType()).referenceInvoiceId(d.getReferenceInvoiceId())
                .creditNoteReason(d.getCreditNoteReason()).overdueNotifiedAt(d.getOverdueNotifiedAt())
                .reminderCount(d.getReminderCount()).version(d.getVersion())
                .createdAt(d.getCreatedAt()).updatedAt(d.getUpdatedAt())
                .build();
    }
}
