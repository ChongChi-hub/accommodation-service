package vn.cozynest.billing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
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
    private long version;
    private Instant createdAt;
    private Instant updatedAt;

    public Invoice(String id, String invoiceCode, String contractId, String utilityReadingId,
                   String tenantId, String landlordId, LocalDate periodMonth,
                   long baseRent, long electricityAmount, long waterAmount,
                   long serviceAmount, long otherAmount, long totalAmount,
                   long paidAmount, LocalDate dueDate, String status, long version,
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
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.dueDate = dueDate;
        this.status = status != null ? status : "ISSUED";
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
                UUID.randomUUID().toString(),
                invoiceCode,
                contractId,
                utilityReadingId,
                tenantId,
                landlordId,
                periodMonth,
                baseRent,
                electricityAmount,
                waterAmount,
                serviceAmount,
                otherAmount,
                total,
                0L,
                dueDate,
                "ISSUED",
                0L,
                Instant.now(),
                Instant.now()
        );
    }

    public void recordPayment(long amount) {
        if ("PAID".equals(this.status)) {
            return; // Immutable once paid (idempotent)
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("So tien thanh toan phai lon hon 0");
        }
        this.paidAmount += amount;
        if (this.paidAmount >= this.totalAmount) {
            this.status = "PAID";
        } else {
            this.status = "PARTIALLY_PAID";
        }
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        if ("PAID".equals(this.status)) {
            throw new IllegalStateException("Khong the huy hoa don da thanh toan");
        }
        this.status = "CANCELLED";
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getInvoiceCode() { return invoiceCode; }
    public String getContractId() { return contractId; }
    public String getUtilityReadingId() { return utilityReadingId; }
    public String getTenantId() { return tenantId; }
    public String getLandlordId() { return landlordId; }
    public LocalDate getPeriodMonth() { return periodMonth; }
    public long getBaseRent() { return baseRent; }
    public long getElectricityAmount() { return electricityAmount; }
    public long getWaterAmount() { return waterAmount; }
    public long getServiceAmount() { return serviceAmount; }
    public long getOtherAmount() { return otherAmount; }
    public long getTotalAmount() { return totalAmount; }
    public long getPaidAmount() { return paidAmount; }
    public LocalDate getDueDate() { return dueDate; }
    public String getStatus() { return status; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
