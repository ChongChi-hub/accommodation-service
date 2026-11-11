package vn.cozynest.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "refund_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequestJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "refund_code", nullable = false, unique = true, length = 64)
    private String refundCode;

    @Column(name = "payment_intent_id", length = 36)
    private String paymentIntentId;

    @Column(name = "contract_id", nullable = false, length = 36)
    private String contractId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "landlord_id", nullable = false, length = 36)
    private String landlordId;

    @Column(nullable = false)
    private long amount;

    @Column(name = "bank_code", nullable = false, length = 30)
    private String bankCode;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "account_holder", nullable = false, length = 150)
    private String accountHolder;

    @Column(name = "otp_verified", nullable = false)
    private boolean otpVerified;

    @Column(name = "kyc_name_matched", nullable = false)
    private boolean kycNameMatched;

    @Column(name = "requires_maker_checker", nullable = false)
    private boolean requiresMakerChecker;

    @Column(name = "maker_checker_request_id", length = 36)
    private String makerCheckerRequestId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

