package vn.toam.billing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    private String id;
    private String invoiceCode;
    private String contractId;
    private String tenantId;
    private String landlordId;
    private LocalDate periodMonth;
    private Long baseRent;
    private Long totalAmount;
    private Long paidAmount;
    private LocalDate dueDate;
    private String status;
    private Long version;
}
