package vn.cozynest.booking.application.port;

/**
 * Outbound port to verify user phone verification status and OTP codes against identity-service.
 */
public interface IdentityVerificationPort {

    boolean isPhoneVerified(String userId);

    boolean verifyContractOtp(String userId, String otpCode);
}

