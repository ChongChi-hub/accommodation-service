package vn.cozynest.billing.application.port;

/**
 * Port to identity-service internal verification APIs for OTP & KYC full-name checks.
 */
public interface BillingIdentityVerificationPort {
    boolean verifyOtp(String userId, String otpCode);

    KycVerificationSummary getKycStatus(String userId);

    record KycVerificationSummary(String userId, String status, boolean approved, String verifiedFullName) {}
}

