package vn.cozynest.booking.application.port;

/**
 * Outbound port to verify user phone verification status, email/KYC status and OTP codes against identity-service.
 */
public interface IdentityVerificationPort {

    boolean isPhoneVerified(String userId);

    boolean verifyContractOtp(String userId, String otpCode);

    UserVerificationStatus getVerificationStatus(String userId);

    default String resolveLandlordUserId(String rawLandlordId) {
        return rawLandlordId;
    }

    record UserVerificationStatus(boolean emailVerified, boolean kycVerified, boolean faceVerified, String role) {}
}

