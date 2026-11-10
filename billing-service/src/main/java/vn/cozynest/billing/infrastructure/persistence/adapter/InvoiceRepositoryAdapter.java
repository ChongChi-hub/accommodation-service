package vn.cozynest.billing.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.billing.domain.model.Invoice;
import vn.cozynest.billing.domain.repository.InvoiceRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.InvoiceJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataInvoiceRepository;

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
        InvoiceJpaEntity entity = toEntity(domain);
        InvoiceJpaEntity saved = repository.save(entity);
        return toDomain(saved);
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

    private Invoice toDomain(InvoiceJpaEntity entity) {
        return new Invoice(
                entity.getId(),
                entity.getInvoiceCode(),
                entity.getContractId(),
                entity.getUtilityReadingId(),
                entity.getTenantId(),
                entity.getLandlordId(),
                entity.getPeriodMonth(),
                entity.getBaseRent(),
                entity.getElectricityAmount(),
                entity.getWaterAmount(),
                entity.getServiceAmount(),
                entity.getOtherAmount(),
                entity.getTotalAmount(),
                entity.getPaidAmount(),
                entity.getDueDate(),
                entity.getStatus(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private InvoiceJpaEntity toEntity(Invoice domain) {
        return InvoiceJpaEntity.builder()
                .id(domain.getId())
                .invoiceCode(domain.getInvoiceCode())
                .contractId(domain.getContractId())
                .utilityReadingId(domain.getUtilityReadingId())
                .tenantId(domain.getTenantId())
                .landlordId(domain.getLandlordId())
                .periodMonth(domain.getPeriodMonth())
                .baseRent(domain.getBaseRent())
                .electricityAmount(domain.getElectricityAmount())
                .waterAmount(domain.getWaterAmount())
                .serviceAmount(domain.getServiceAmount())
                .otherAmount(domain.getOtherAmount())
                .totalAmount(domain.getTotalAmount())
                .paidAmount(domain.getPaidAmount())
                .dueDate(domain.getDueDate())
                .status(domain.getStatus())
                .version(domain.getVersion())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

