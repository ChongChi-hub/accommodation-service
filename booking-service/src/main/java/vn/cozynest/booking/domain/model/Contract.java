package vn.cozynest.booking.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contract {
    private String id;
    private String contractCode;
    private String roomId;
    private String tenantId;
    private String landlordId;
    private String requestId;
    private LocalDate startDate;
    private LocalDate endDate;
    private long monthlyRent;
    private long depositAmount;
    private int paymentCycleMonths;
    private int paymentDueDay;
    private String terms;
    private String documentHash;
    private String pdfUrl;
    private String status;
    private Instant holdingExpiresAt;
    private Instant tenantSignedAt;
    private Instant landlordSignedAt;
    private long version;
    private Instant createdAt;
    private Instant updatedAt;

    public static Contract create(String contractCode, String roomId, String tenantId, String landlordId,
                                  String requestId, LocalDate startDate, LocalDate endDate,
                                  long monthlyRent, long depositAmount, String terms) {
        String hash = computeHash(contractCode + terms + startDate + endDate + monthlyRent + depositAmount);
        return Contract.builder()
                .id(UUID.randomUUID().toString())
                .contractCode(contractCode)
                .roomId(roomId)
                .tenantId(tenantId)
                .landlordId(landlordId)
                .requestId(requestId)
                .startDate(startDate)
                .endDate(endDate)
                .monthlyRent(monthlyRent)
                .depositAmount(depositAmount)
                .paymentCycleMonths(1)
                .paymentDueDay(5)
                .terms(terms)
                .documentHash(hash)
                .status("PENDING_SIGN")
                .holdingExpiresAt(Instant.now().plusSeconds(86400)) // 24h
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public void signByTenant() {
        if ("ACTIVE".equals(this.status) || "CANCELLED".equals(this.status)) {
            return; // Idempotent
        }
        this.tenantSignedAt = Instant.now();
        if (this.landlordSignedAt != null) {
            this.status = "ACTIVE";
        }
        this.updatedAt = Instant.now();
    }

    public void signByLandlord() {
        if ("ACTIVE".equals(this.status) || "CANCELLED".equals(this.status)) {
            return;
        }
        this.landlordSignedAt = Instant.now();
        if (this.tenantSignedAt != null) {
            this.status = "ACTIVE";
        }
        this.updatedAt = Instant.now();
    }

    public void terminate() {
        this.status = "TERMINATED";
        this.updatedAt = Instant.now();
    }

    public static String computeHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "";
        }
    }
}
