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
    private long lateFeeAmount;
    private long creditAppliedAmount;
    private long totalAmount;
    private long paidAmount;
    private long overpaymentCreditAmount;
    private LocalDate dueDate;
    private String status;
    private String invoiceType;
    private String referenceInvoiceId;
    private String creditNoteReason;
    private Instant overdueNotifiedAt;
    private int reminderCount;
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
                baseRent, electricityAmount, waterAmount, serviceAmount, otherAmount, 0L, 0L,
                totalAmount, paidAmount, 0L, dueDate, status, "STANDARD", null, null, null, 0,
                version, createdAt, updatedAt);
    }

    public Invoice(String id, String invoiceCode, String contractId, String utilityReadingId,
                   String tenantId, String landlordId, LocalDate periodMonth,
                   long baseRent, long electricityAmount, long waterAmount,
                   long serviceAmount, long otherAmount, long totalAmount,
                   long paidAmount, LocalDate dueDate, String status,
                   String invoiceType, String referenceInvoiceId, String creditNoteReason,
                   Instant overdueNotifiedAt, long version, Instant createdAt, Instant updatedAt) {
        this(id, invoiceCode, contractId, utilityReadingId, tenantId, landlordId, periodMonth,
                baseRent, electricityAmount, waterAmount, serviceAmount, otherAmount, 0L, 0L,
                totalAmount, paidAmount, 0L, dueDate, status, invoiceType, referenceInvoiceId,
                creditNoteReason, overdueNotifiedAt, 0, version, createdAt, updatedAt);
    }

    @Builder
    public Invoice(String id, String invoiceCode, String contractId, String utilityReadingId,
                   String tenantId, String landlordId, LocalDate periodMonth,
                   long baseRent, long electricityAmount, long waterAmount,
                   long serviceAmount, long otherAmount, long lateFeeAmount, long creditAppliedAmount,
                   long totalAmount, long paidAmount, long overpaymentCreditAmount,
                   LocalDate dueDate, String status,
                   String invoiceType, String referenceInvoiceId, String creditNoteReason,
                   Instant overdueNotifiedAt, int reminderCount, long version,
                   Instant createdAt, Instant updatedAt) {
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
        this.lateFeeAmount = lateFeeAmount;
        this.creditAppliedAmount = creditAppliedAmount;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.overpaymentCreditAmount = overpaymentCreditAmount;
        this.dueDate = dueDate;
        this.status = status != null ? status : "ISSUED";
        this.invoiceType = invoiceType != null ? invoiceType : "STANDARD";
        this.referenceInvoiceId = referenceInvoiceId;
        this.creditNoteReason = creditNoteReason;
        this.overdueNotifiedAt = overdueNotifiedAt;
        this.reminderCount = reminderCount;
        this.version = version;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Invoice create(String invoiceCode, String contractId, String utilityReadingId,
                                 String tenantId, String landlordId, LocalDate periodMonth,
                                 long baseRent, long electricityAmount, long waterAmount,
                                 long serviceAmount, long otherAmount, LocalDate dueDate) {
        return createWithCredits(invoiceCode, contractId, utilityReadingId, tenantId, landlordId,
                periodMonth, baseRent, electricityAmount, waterAmount, serviceAmount, otherAmount, 0L, dueDate);
    }

    public static Invoice createWithCredits(String invoiceCode, String contractId, String utilityReadingId,
                                            String tenantId, String landlordId, LocalDate periodMonth,
                                            long baseRent, long electricityAmount, long waterAmount,
                                            long serviceAmount, long otherAmount, long creditAppliedAmount,
                                            LocalDate dueDate) {
        long raw = baseRent + electricityAmount + waterAmount + serviceAmount + otherAmount;
        long effectiveCredit = Math.max(0L, Math.min(raw, creditAppliedAmount));
        long total = raw - effectiveCredit;
        return new Invoice(
                UUID.randomUUID().toString(), invoiceCode, contractId, utilityReadingId,
                tenantId, landlordId, periodMonth, baseRent, electricityAmount, waterAmount,
                serviceAmount, otherAmount, 0L, effectiveCredit, total, 0L, 0L, dueDate, "ISSUED",
                "STANDARD", null, null, null, 0, 0L, Instant.now(), Instant.now()
        );
    }

    public static Invoice createCreditNote(Invoice target, String creditCode, long creditAmount, String reason) {
        if (creditAmount <= 0 || creditAmount > target.getTotalAmount()) {
            throw new IllegalArgumentException("So tien dieu chinh giam khong hop le");
        }
        return new Invoice(
                UUID.randomUUID().toString(), creditCode, target.getContractId(), null,
                target.getTenantId(), target.getLandlordId(), target.getPeriodMonth(),
                0L, 0L, 0L, 0L, -creditAmount, 0L, 0L, -creditAmount, 0L, 0L, target.getDueDate(), "ISSUED",
                "CREDIT_NOTE", target.getId(), reason, null, 0, 0L, Instant.now(), Instant.now()
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
        if (this.paidAmount >= this.totalAmount) {
            long surplus = this.paidAmount - this.totalAmount;
            if (surplus > 0) {
                this.overpaymentCreditAmount += surplus;
                this.paidAmount = this.totalAmount;
            }
            this.status = "PAID";
        } else {
            this.status = "PARTIALLY_PAID";
        }
        this.updatedAt = Instant.now();
    }

    public void consumeOverpaymentCredit() {
        this.overpaymentCreditAmount = 0L;
        this.updatedAt = Instant.now();
    }

    public void applyLateFee(long feeAmount) {
        if (feeAmount <= 0) {
            throw new IllegalArgumentException("Phi tre han phai lon hon 0");
        }
        if ("PAID".equals(this.status) || "CANCELLED".equals(this.status)) {
            throw new IllegalStateException("Khong the ap phi tre han cho hoa don da dong hoac da huy");
        }
        if (this.lateFeeAmount > 0) {
            throw new IllegalStateException("Hoa don da duoc ap phi tre han");
        }
        this.lateFeeAmount = feeAmount;
        this.totalAmount += feeAmount;
        this.updatedAt = Instant.now();
    }

    public void markOverdue(LocalDate today) {
        if (("ISSUED".equals(this.status) || "PARTIALLY_PAID".equals(this.status) || "OVERDUE".equals(this.status))
                && this.dueDate != null && today.isAfter(this.dueDate) && this.reminderCount < 3) {
            this.status = "OVERDUE";
            this.overdueNotifiedAt = Instant.now();
            this.reminderCount += 1;
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
