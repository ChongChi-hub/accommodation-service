package vn.cozynest.billing.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.InvoiceDto;
import vn.cozynest.billing.domain.model.Invoice;
import vn.cozynest.billing.domain.repository.InvoiceRepository;

import java.util.List;
import java.util.UUID;

@Service
public class InvoiceService {
    private final InvoiceRepository repository;

    public InvoiceService(InvoiceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public InvoiceDto.InvoiceResponse generateInvoice(InvoiceDto.GenerateInvoiceCommand command) {
        String invoiceCode = "INV-" + command.periodMonth().getYear()
                + String.format("%02d", command.periodMonth().getMonthValue())
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Invoice invoice = Invoice.create(
                invoiceCode,
                command.contractId(),
                command.utilityReadingId(),
                command.tenantId(),
                command.landlordId(),
                command.periodMonth(),
                command.baseRent(),
                command.electricityAmount(),
                command.waterAmount(),
                command.serviceAmount(),
                command.otherAmount(),
                command.dueDate()
        );

        Invoice saved = repository.save(invoice);
        return toResponse(saved);
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
    public InvoiceDto.InvoiceResponse getInvoiceById(String id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn: " + id));
    }

    @Transactional
    public void cancelInvoice(String id) {
        Invoice invoice = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn: " + id));
        invoice.cancel();
        repository.save(invoice);
    }

    private InvoiceDto.InvoiceResponse toResponse(Invoice i) {
        return new InvoiceDto.InvoiceResponse(
                i.getId(), i.getInvoiceCode(), i.getContractId(), i.getUtilityReadingId(),
                i.getTenantId(), i.getLandlordId(), i.getPeriodMonth(),
                i.getBaseRent(), i.getElectricityAmount(), i.getWaterAmount(),
                i.getServiceAmount(), i.getOtherAmount(), i.getTotalAmount(),
                i.getPaidAmount(), i.getDueDate(), i.getStatus(), i.getCreatedAt()
        );
    }
}

