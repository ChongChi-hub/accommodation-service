package vn.cozynest.identity.application.port;

public interface SmsSenderPort {
    void sendOtpSms(String phoneNumber, String otp);
}

