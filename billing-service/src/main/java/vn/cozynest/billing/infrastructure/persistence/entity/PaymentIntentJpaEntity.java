package vn.cozynest.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "payment_intents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "payment_code", nullable = false, unique = true, length = 64)
    private String paymentCode;

    @Column(name = "payer_id", nullable = false, length = 36)
    private String payerId;

    @Column(name = "order_type", nullable = false, length = 50)
    private String orderType;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(nullable = false)
    private long amount;

    @Column(name = "paid_amount", nullable = false)
    @Builder.Default
    private long paidAmount = 0L;

    @Column(name = "qr_url", length = 500)
    private String qrUrl;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

