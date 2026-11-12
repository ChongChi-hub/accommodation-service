package vn.cozynest.booking.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "contract_signature_audits")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractSignatureAuditJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "contract_id", nullable = false, length = 36)
    private String contractId;

    @Column(name = "signer_id", nullable = false, length = 36)
    private String signerId;

    @Column(name = "signer_role", nullable = false, length = 20)
    private String signerRole;

    @Column(name = "otp_verified", nullable = false)
    private boolean otpVerified;

    @Column(name = "document_hash", nullable = false, length = 64)
    private String documentHash;

    @Column(name = "pdf_object_key", length = 255)
    private String pdfObjectKey;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "signed_at", nullable = false)
    private Instant signedAt;
}

