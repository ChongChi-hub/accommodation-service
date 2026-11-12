package vn.cozynest.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "payout_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutRequestJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "payout_code", nullable = false, unique = true, length = 64)
    private String payoutCode;

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

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "REQUESTED";

    @Column(name = "otp_verified", nullable = false)
    @Builder.Default
    private boolean otpVerified = false;

    @Column(name = "requires_maker_checker", nullable = false)
    @Builder.Default
    private boolean requiresMakerChecker = false;

    @Column(name = "maker_checker_request_id", length = 36)
    private String makerCheckerRequestId;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

