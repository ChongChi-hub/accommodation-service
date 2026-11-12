package vn.cozynest.booking.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractSignatureAudit {
    private String id;
    private String contractId;
    private String signerId;
    private String signerRole;
    private boolean otpVerified;
    private String documentHash;
    private String pdfObjectKey;
    private String ipAddress;
    private String userAgent;
    private Instant signedAt;

    public static ContractSignatureAudit record(
            String contractId,
            String signerId,
            String signerRole,
            boolean otpVerified,
            String documentHash,
            String pdfObjectKey,
            String ipAddress,
            String userAgent
    ) {
        return ContractSignatureAudit.builder()
                .id(UUID.randomUUID().toString())
                .contractId(contractId)
                .signerId(signerId)
                .signerRole(signerRole)
                .otpVerified(otpVerified)
                .documentHash(documentHash)
                .pdfObjectKey(pdfObjectKey)
                .ipAddress(ipAddress != null && ipAddress.length() > 64 ? ipAddress.substring(0, 64) : ipAddress)
                .userAgent(userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent)
                .signedAt(Instant.now())
                .build();
    }
}

