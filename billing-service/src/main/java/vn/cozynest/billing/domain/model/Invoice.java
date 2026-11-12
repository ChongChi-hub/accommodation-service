package vn.cozynest.billing.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class Invoice {
    private String id;
    private String invoiceCode;
    private String contractId;
    private String utilityReadingId;
    private String tenantId;
    private String landlordId;
    private LocalDate periodMonth;
    private long baseRent;
    private long electricityAmount;
    private long waterAmount;
    private long serviceAmount;
    private long otherAmount;
    private long totalAmount;
    private long paidAmount;
    private LocalDate dueDate;
    private String status;
    private String invoiceType;
    private String referenceInvoiceId;
    private String creditNoteReason;
    private Instant overdueNotifiedAt;
    private long version;
    private Instant createdAt;
    private Instant updatedAt;

    public Invoice(String id, String invoiceCode, String contractId, String utilityReadingId,
                   String tenantId, String landlordId, LocalDate periodMonth,
                   long baseRent, long electricityAmount, long waterAmount,
                   long serviceAmount, long otherAmount, long totalAmount,
                   long paidAmount, LocalDate dueDate, String status, long version,
                   Instant createdAt, Instant updatedAt) {
        this(id, invoiceCode, contractId, utilityReadingId, tenantId, landlordId, periodMonth,
                baseRent, electricityAmount, waterAmount, serviceAmount, otherAmount, totalAmount,
                paidAmount, dueDate, status, "STANDARD", null, null, null, version, createdAt, updatedAt);
    }

    @Builder
    public Invoice(String id, String invoiceCode, String contractId, String utilityReadingId,
                   String tenantId, String landlordId, LocalDate periodMonth,
                   long baseRent, long electricityAmount, long waterAmount,
                   long serviceAmount, long otherAmount, long totalAmount,
                   long paidAmount, LocalDate dueDate, String status,
                   String invoiceType, String referenceInvoiceId, String creditNoteReason,
                   Instant overdueNotifiedAt, long version, Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.invoiceCode = invoiceCode;
        this.contractId = contractId;
        this.utilityReadingId = utilityReadingId;
        this.tenantId = tenantId;
        this.landlordId = landlordId;
        this.periodMonth = periodMonth;
        this.baseRent = baseRent;
        this.electricityAmount = electricityAmount;
        this.waterAmount = waterAmount;
        this.serviceAmount = serviceAmount;
        this.otherAmount = otherAmount;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.dueDate = dueDate;
        this.status = status != null ? status : "ISSUED";
        this.invoiceType = invoiceType != null ? invoiceType : "STANDARD";
        this.referenceInvoiceId = referenceInvoiceId;
        this.creditNoteReason = creditNoteReason;
        this.overdueNotifiedAt = overdueNotifiedAt;
        this.version = version;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Invoice create(String invoiceCode, String contractId, String utilityReadingId,
                                 String tenantId, String landlordId, LocalDate periodMonth,
                                 long baseRent, long electricityAmount, long waterAmount,
                                 long serviceAmount, long otherAmount, LocalDate dueDate) {
        long total = baseRent + electricityAmount + waterAmount + serviceAmount + otherAmount;
        return new Invoice(
                UUID.randomUUID().toString(), invoiceCode, contractId, utilityReadingId,
                tenantId, landlordId, periodMonth, baseRent, electricityAmount, waterAmount,
                serviceAmount, otherAmount, total, 0L, dueDate, "ISSUED",
                "STANDARD", null, null, null, 0L, Instant.now(), Instant.now()
        );
    }

    public static Invoice createCreditNote(Invoice target, String creditCode, long creditAmount, String reason) {
        if (creditAmount <= 0 || creditAmount > target.getTotalAmount()) {
            throw new IllegalArgumentException("So tien dieu chinh giam khong hop le");
        }
        return new Invoice(
                UUID.randomUUID().toString(), creditCode, target.getContractId(), null,
                target.getTenantId(), target.getLandlordId(), target.getPeriodMonth(),
                0L, 0L, 0L, 0L, -creditAmount, -creditAmount, 0L, target.getDueDate(), "ISSUED",
                "CREDIT_NOTE", target.getId(), reason, null, 0L, Instant.now(), Instant.now()
        );
    }

    public void recordPayment(long amount) {
        if ("PAID".equals(this.status) || "CANCELLED".equals(this.status)) {
            return;
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("So tien thanh toan phai lon hon 0");
        }
        this.paidAmount += amount;
        this.status = this.paidAmount >= this.totalAmount ? "PAID" : "PARTIALLY_PAID";
        this.updatedAt = Instant.now();
    }

    public void markOverdue(LocalDate today) {
        if (("ISSUED".equals(this.status) || "PARTIALLY_PAID".equals(this.status))
                && this.dueDate != null && today.isAfter(this.dueDate)) {
            this.status = "OVERDUE";
            this.overdueNotifiedAt = Instant.now();
            this.updatedAt = Instant.now();
        }
    }

    public void cancel() {
        if ("PAID".equals(this.status)) {
            throw new IllegalStateException("Khong the huy hoa don da thanh toan");
        }
        this.status = "CANCELLED";
        this.updatedAt = Instant.now();
    }
}
