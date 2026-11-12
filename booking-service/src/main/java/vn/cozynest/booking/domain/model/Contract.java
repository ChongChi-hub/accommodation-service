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
    private String verificationCode;
    private String pdfUrl;
    private String pdfObjectKey;
    private String status;
    private String expiryNotifiedDays;
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
                .verificationCode("CN-" + contractCode)
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

    /**
     * Allows landlord/creator to update contract draft details ONLY before any signature is recorded.
     */
    public void updateDraft(LocalDate startDate, LocalDate endDate, long monthlyRent, long depositAmount, String terms) {
        if ("ACTIVE".equals(this.status) || "TERMINATED".equals(this.status) || "CANCELLED".equals(this.status)
                || this.tenantSignedAt != null || this.landlordSignedAt != null) {
            throw new IllegalStateException("Khong the chinh sua hop dong da duoc ky hoac da kich hoat");
        }
        if (startDate != null) {
            this.startDate = startDate;
        }
        if (endDate != null) {
            this.endDate = endDate;
        }
        if (this.endDate != null && this.startDate != null && !this.endDate.isAfter(this.startDate)) {
            throw new IllegalArgumentException("Ngay ket thuc hop dong phai sau ngay bat dau");
        }
        if (monthlyRent > 0) {
            this.monthlyRent = monthlyRent;
        }
        if (depositAmount >= 0) {
            this.depositAmount = depositAmount;
        }
        if (terms != null) {
            this.terms = terms;
        }
        this.documentHash = computeCanonicalHash();
        this.updatedAt = Instant.now();
    }

    public void recordPdfStorage(String pdfObjectKey, String signedUrlPath, String sha256Hex) {
        this.pdfObjectKey = pdfObjectKey;
        this.pdfUrl = signedUrlPath;
        if (sha256Hex != null && !sha256Hex.isBlank()) {
            this.documentHash = sha256Hex;
        }
        this.updatedAt = Instant.now();
    }

    public void applyAmendment(Long newRent, LocalDate newEndDate) {
        if (newRent != null && newRent > 0) {
            this.monthlyRent = newRent;
        }
        if (newEndDate != null && (this.startDate == null || newEndDate.isAfter(this.startDate))) {
            this.endDate = newEndDate;
        }
        this.documentHash = computeCanonicalHash();
        this.updatedAt = Instant.now();
    }

    public String computeCanonicalHash() {
        return computeHash(contractCode + "|" + roomId + "|" + tenantId + "|" + landlordId
                + "|" + startDate + "|" + endDate + "|" + monthlyRent + "|" + depositAmount
                + "|" + (terms != null ? terms : ""));
    }

    public boolean hasNotifiedDays(int daysRemaining) {
        String marker = "," + daysRemaining + ",";
        return this.expiryNotifiedDays != null && this.expiryNotifiedDays.contains(marker);
    }

    public void markNotifiedDays(int daysRemaining) {
        String base = (this.expiryNotifiedDays == null || this.expiryNotifiedDays.isBlank()) ? "," : this.expiryNotifiedDays;
        String token = daysRemaining + ",";
        if (!base.contains("," + token)) {
            this.expiryNotifiedDays = base + token;
            this.updatedAt = Instant.now();
        }
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
