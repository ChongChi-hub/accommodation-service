package vn.cozynest.billing.domain.model;

import java.time.Instant;
import java.time.LocalDate;

public class RiskProfile {
    private final String userId;
    private long dailyLimit;
    private long dailyWithdrawnAmount;
    private LocalDate lastWithdrawalDate;
    private int velocityCount;
    private boolean frozen;
    private String freezeReason;
    private Instant updatedAt;

    public RiskProfile(String userId, long dailyLimit, long dailyWithdrawnAmount,
                       LocalDate lastWithdrawalDate, int velocityCount,
                       boolean frozen, String freezeReason, Instant updatedAt) {
        this.userId = userId;
        this.dailyLimit = dailyLimit > 0 ? dailyLimit : 20_000_000L;
        this.dailyWithdrawnAmount = dailyWithdrawnAmount;
        this.lastWithdrawalDate = lastWithdrawalDate;
        this.velocityCount = velocityCount;
        this.frozen = frozen;
        this.freezeReason = freezeReason;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static RiskProfile createDefault(String userId) {
        return new RiskProfile(userId, 20_000_000L, 0L, null, 0, false, null, Instant.now());
    }

    public void validateWithdrawal(long amount, LocalDate today) {
        if (this.frozen) {
            throw new IllegalStateException("Tai khoan dang bi dong bang: " + (freezeReason != null ? freezeReason : "Vi pham quy tac an toan"));
        }
        long currentDayWithdrawn = today.equals(lastWithdrawalDate) ? this.dailyWithdrawnAmount : 0L;
        if (currentDayWithdrawn + amount > this.dailyLimit) {
            throw new IllegalStateException("So tien rut vuot qua han muc ngay (" + dailyLimit + " VND)");
        }
    }

    public void recordWithdrawal(long amount, LocalDate today) {
        if (today.equals(lastWithdrawalDate)) {
            this.dailyWithdrawnAmount += amount;
            this.velocityCount++;
        } else {
            this.dailyWithdrawnAmount = amount;
            this.lastWithdrawalDate = today;
            this.velocityCount = 1;
        }
        this.updatedAt = Instant.now();
    }

    public void freeze(String reason) {
        this.frozen = true;
        this.freezeReason = reason;
        this.updatedAt = Instant.now();
    }

    public void unfreeze() {
        this.frozen = false;
        this.freezeReason = null;
        this.updatedAt = Instant.now();
    }

    public String getUserId() { return userId; }
    public long getDailyLimit() { return dailyLimit; }
    public long getDailyWithdrawnAmount() { return dailyWithdrawnAmount; }
    public LocalDate getLastWithdrawalDate() { return lastWithdrawalDate; }
    public int getVelocityCount() { return velocityCount; }
    public boolean isFrozen() { return frozen; }
    public String getFreezeReason() { return freezeReason; }
    public Instant getUpdatedAt() { return updatedAt; }
}

