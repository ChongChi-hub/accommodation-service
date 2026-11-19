package vn.cozynest.billing.domain.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

public class UserBankAccount {
    private final String id;
    private final String userId;
    private String bankCode;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private String qrCodeUrl;
    private String verificationStatus;
    private final Instant createdAt;
    private Instant updatedAt;

    public UserBankAccount(String id, String userId, String bankCode, String bankName,
                           String accountNumber, String accountHolder, String qrCodeUrl,
                           String verificationStatus, Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.userId = userId;
        this.bankCode = bankCode;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.qrCodeUrl = qrCodeUrl;
        this.verificationStatus = verificationStatus != null ? verificationStatus : "MATCHED";
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static UserBankAccount create(String userId, String bankCode, String bankName,
                                         String accountNumber, String accountHolder, String qrCodeUrl) {
        Instant now = Instant.now();
        return new UserBankAccount(UUID.randomUUID().toString(), userId, bankCode, bankName,
                accountNumber, accountHolder, qrCodeUrl, "MATCHED", now, now);
    }

    public void update(String bankCode, String bankName, String accountNumber,
                       String accountHolder, String qrCodeUrl, String verificationStatus) {
        this.bankCode = bankCode;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.qrCodeUrl = qrCodeUrl;
        if (verificationStatus != null && !verificationStatus.isBlank()) {
            this.verificationStatus = verificationStatus;
        }
        this.updatedAt = Instant.now();
    }

    public boolean isMatched() {
        return "MATCHED".equalsIgnoreCase(this.verificationStatus)
                && accountNumber != null && !accountNumber.isBlank()
                && bankCode != null && !bankCode.isBlank();
    }

    public String getLast4Digits() {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber != null ? accountNumber : "";
        }
        return accountNumber.substring(accountNumber.length() - 4);
    }

    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        return "******" + getLast4Digits();
    }

    public String buildSystemVietQrUrl(long amount, String addInfo) {
        String encodedHolder = URLEncoder.encode(accountHolder != null ? accountHolder : "", StandardCharsets.UTF_8);
        String encodedInfo = URLEncoder.encode(addInfo != null ? addInfo : "", StandardCharsets.UTF_8);
        return "https://img.vietqr.io/image/" + bankCode + "-" + accountNumber
                + "-compact2.png?amount=" + Math.max(0L, amount)
                + "&addInfo=" + encodedInfo
                + "&accountName=" + encodedHolder;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getBankCode() { return bankCode; }
    public String getBankName() { return bankName; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountHolder() { return accountHolder; }
    public String getQrCodeUrl() { return qrCodeUrl; }
    public String getVerificationStatus() { return verificationStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

