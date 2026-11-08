package vn.cozynest.identity.application.port;

public interface EmailServicePort {
    void sendPasswordResetOtp(String toEmail, String fullName, String otpCode);
    void sendPasswordChangedNotice(String toEmail, String fullName);
    void sendEmailVerificationOtp(String toEmail, String fullName, String otpCode);
}
